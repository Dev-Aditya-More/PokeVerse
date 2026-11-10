#!/usr/bin/env node
/**
 * Developer-only admin chores, run from YOUR machine with YOUR Firebase CLI login.
 * They replace the old public admin HTTP functions: nothing here is deployed,
 * so there is no URL anyone else can call. Requires `firebase login` with project access.
 *
 *   node scripts/admin.js app-config --latest 97 --min 0 --name 2.2.6
 *       Sets leaderboard/config. latest = soft "update available" dialog,
 *       min = hard block (app refuses to run below this). Double-check `--min`!
 *
 *   node scripts/admin.js clean-leaderboard [--apply]
 *       Removes personal/invalid data from public leaderboard docs:
 *       the legacy `email` field, and non-https photo URLs (local content:// paths).
 *       Dry run by default; pass --apply to write.
 *
 *   node scripts/admin.js grant-coins --amount 10000 --id promo20261110 --emails a@x.com,b@y.com [--apply]
 *       Adds Poké Coins to the accounts with those emails (found through Firebase Auth, then
 *       users/{uid}.progression.earned). Coins are only
 *       ever "earned" upward, so the apps merge it in the next time they sync. Each grant has an --id
 *       (letters and digits) that is recorded on the user doc, so running it twice for the same id does
 *       nothing the second time. Dry run by default: it only reports who it found; pass --apply to write.
 */
const { execSync } = require("child_process");
const path = require("path");

const PROJECT = "composepractice-33177";
const BASE = `https://firestore.googleapis.com/v1/projects/${PROJECT}/databases/(default)/documents`;

async function accessToken() {
  const ft = path.join(execSync("npm root -g").toString().trim(), "firebase-tools", "lib");
  const { requireAuth } = require(path.join(ft, "requireAuth"));
  const { getAccessToken } = require(path.join(ft, "apiv2"));
  const acct = require(path.join(ft, "auth")).getGlobalDefaultAccount();
  if (!acct) throw new Error("Not logged in: run `firebase login` first");
  await requireAuth({ project: PROJECT, user: acct.user, tokens: acct.tokens });
  return getAccessToken();
}

async function api(token, url, init = {}) {
  const res = await fetch(url, {
    ...init,
    headers: { "Authorization": `Bearer ${token}`, "Content-Type": "application/json" },
  });
  const body = await res.json();
  if (!res.ok) throw new Error(`${res.status} ${JSON.stringify(body.error ?? body)}`);
  return body;
}

function arg(name) {
  const i = process.argv.indexOf(`--${name}`);
  return i > 0 ? process.argv[i + 1] : undefined;
}

async function appConfig(token) {
  const latest = parseInt(arg("latest") ?? "", 10);
  const min = parseInt(arg("min") ?? "0", 10);
  const name = arg("name") ?? "";
  if (!latest || Number.isNaN(min) || min > latest) {
    throw new Error("usage: app-config --latest <versionCode> [--min <versionCode>] [--name 2.2.6] (min <= latest)");
  }
  const mask = ["latestVersionCode", "minVersionCode", "latestVersionName"]
      .map((f) => `updateMask.fieldPaths=${f}`).join("&");
  await api(token, `${BASE}/leaderboard/config?${mask}`, {
    method: "PATCH",
    body: JSON.stringify({ fields: {
      latestVersionCode: { integerValue: String(latest) },
      minVersionCode: { integerValue: String(min) },
      latestVersionName: { stringValue: name },
    } }),
  });
  console.log(`✅ leaderboard/config: latest=${latest} min=${min} name="${name}"`);
}

async function cleanLeaderboard(token) {
  const apply = process.argv.includes("--apply");
  const docs = [];
  let pageToken = "";
  do {
    const page = await api(token, `${BASE}/leaderboard?pageSize=300${pageToken ? `&pageToken=${pageToken}` : ""}`);
    docs.push(...(page.documents ?? []));
    pageToken = page.nextPageToken ?? "";
  } while (pageToken);

  const writes = [];
  for (const d of docs) {
    const f = d.fields ?? {};
    const fields = {};
    const mask = [];
    if ("email" in f) mask.push("email"); // masked + absent from `fields` = delete the field
    const photo = f.photoUrl?.stringValue;
    if (photo && !photo.startsWith("https://")) {
      mask.push("photoUrl");
      fields.photoUrl = { stringValue: "" };
    }
    if (mask.length) writes.push({ update: { name: d.name, fields }, updateMask: { fieldPaths: mask } });
  }

  console.log(`${docs.length} docs scanned, ${writes.length} need cleaning` +
      ` (email: ${writes.filter((w) => w.updateMask.fieldPaths.includes("email")).length},` +
      ` bad photoUrl: ${writes.filter((w) => w.updateMask.fieldPaths.includes("photoUrl")).length})`);
  if (!apply) return console.log("Dry run. Re-run with --apply to write.");

  for (let i = 0; i < writes.length; i += 400) {
    await api(token, `${BASE}:commit`, {
      method: "POST",
      body: JSON.stringify({ writes: writes.slice(i, i + 400) }),
    });
  }
  console.log(`✅ Cleaned ${writes.length} docs`);
}

/**
 * Finds the account for an email through Firebase Authentication (the users/{uid} documents deliberately
 * do not store emails), then loads that account's users/{uid} document. Returns { uid, doc } where doc is
 * null if the account has never synced a profile; or null if no account has the email.
 */
async function findUserByEmail(token, email) {
  const res = await api(token, `https://identitytoolkit.googleapis.com/v1/projects/${PROJECT}/accounts:lookup`, {
    method: "POST",
    body: JSON.stringify({ email: [email.toLowerCase()] }),
  });
  const account = (res.users ?? [])[0];
  if (!account) return null;
  const uid = account.localId;
  let doc = null;
  try {
    doc = await api(token, `${BASE}/users/${uid}`);
  } catch (e) {
    if (!String(e.message).startsWith("404")) throw e;
  }
  return { uid, doc };
}

async function grantCoins(token) {
  const apply = process.argv.includes("--apply");
  const amount = parseInt(arg("amount") ?? "", 10);
  const id = arg("id") ?? "";
  const emails = (arg("emails") ?? "").split(",").map((e) => e.trim()).filter(Boolean);
  if (!(amount > 0 && amount <= 100000) || !/^[A-Za-z0-9]{3,40}$/.test(id) || emails.length === 0) {
    throw new Error("usage: grant-coins --amount <1..100000> --id <letters/digits> --emails a@x.com,b@y.com [--apply]");
  }

  for (const email of emails) {
    const found = await findUserByEmail(token, email);
    if (!found) { console.log(`⚠️  ${email}: no Firebase account with this email (did they sign in with a different address?)`); continue; }
    const { uid, doc } = found;
    const fields = doc?.fields ?? {};
    if (fields.coinGrants?.mapValue?.fields?.[id]) { console.log(`⏭️  ${email} (${uid}): grant "${id}" already applied`); continue; }

    const progression = fields.progression?.mapValue?.fields ?? {};
    const earned = parseInt(progression.earned?.integerValue ?? "0", 10);
    const spent = parseInt(progression.spent?.integerValue ?? "0", 10);
    const next = earned + amount;
    console.log(`${apply ? "➕" : "🔎"} ${email} (${uid}): earned ${earned} -> ${next} (balance ${earned - spent} -> ${next - spent})`);
    if (!apply) continue;

    // The precondition makes this fail instead of overwriting if the account changed since it was read
    // (or, for an account with no profile document yet, if one appeared in the meantime).
    await api(token, `${BASE}:commit`, {
      method: "POST",
      body: JSON.stringify({
        writes: [{
          update: {
            name: `projects/${PROJECT}/databases/(default)/documents/users/${uid}`,
            fields: {
              progression: { mapValue: { fields: { earned: { integerValue: String(next) } } } },
              coinGrants: { mapValue: { fields: { [id]: { booleanValue: true } } } },
            },
          },
          updateMask: { fieldPaths: ["progression.earned", `coinGrants.${id}`] },
          currentDocument: doc ? { updateTime: doc.updateTime } : { exists: false },
        }],
      }),
    });
    console.log(`   ✅ granted ${amount} coins`);
  }
  if (!apply) console.log("Dry run. Re-run with --apply to write.");
}

(async () => {
  const cmd = process.argv[2];
  const commands = { "app-config": appConfig, "clean-leaderboard": cleanLeaderboard, "grant-coins": grantCoins };
  if (!commands[cmd]) {
    console.log("commands: app-config | clean-leaderboard | grant-coins  (see header of scripts/admin.js)");
    process.exit(1);
  }
  await commands[cmd](await accessToken());
})().catch((e) => {
  console.error("❌", e.message);
  process.exit(1);
});
