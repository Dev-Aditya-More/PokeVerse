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

(async () => {
  const cmd = process.argv[2];
  const commands = { "app-config": appConfig, "clean-leaderboard": cleanLeaderboard };
  if (!commands[cmd]) {
    console.log("commands: app-config | clean-leaderboard  (see header of scripts/admin.js)");
    process.exit(1);
  }
  await commands[cmd](await accessToken());
})().catch((e) => {
  console.error("❌", e.message);
  process.exit(1);
});
