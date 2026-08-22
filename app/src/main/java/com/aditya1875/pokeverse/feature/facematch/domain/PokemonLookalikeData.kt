package com.aditya1875.pokeverse.feature.facematch.domain

/**
 * A curated, hand-picked pool of well-known Pokémon tagged with an approximate dominant
 * color and a rough "face shape" bucket. This isn't scientific — it's a fun heuristic match,
 * not real face recognition — so the pool is deliberately small and recognizable rather than
 * exhaustive (all 1000+ Pokémon would need this tagging done by hand too).
 */
object PokemonLookalikeData {

    val all: List<PokemonLookalike> = listOf(
        PokemonLookalike(25, "Pikachu", RgbColor(0.95f, 0.82f, 0.20f), FaceShape.ROUND, "Bright, energetic, impossible to ignore."),
        PokemonLookalike(1, "Bulbasaur", RgbColor(0.35f, 0.65f, 0.45f), FaceShape.ROUND, "Calm, grounded, quietly dependable."),
        PokemonLookalike(4, "Charmander", RgbColor(0.90f, 0.45f, 0.25f), FaceShape.OVAL, "Warm, spirited, a little fiery."),
        PokemonLookalike(7, "Squirtle", RgbColor(0.35f, 0.60f, 0.85f), FaceShape.ROUND, "Cool-headed and easygoing."),
        PokemonLookalike(39, "Jigglypuff", RgbColor(0.95f, 0.75f, 0.80f), FaceShape.ROUND, "Soft features, big expressive eyes."),
        PokemonLookalike(143, "Snorlax", RgbColor(0.30f, 0.35f, 0.45f), FaceShape.ROUND, "Relaxed, comfortable, unbothered by chaos."),
        PokemonLookalike(94, "Gengar", RgbColor(0.45f, 0.30f, 0.55f), FaceShape.OVAL, "Mysterious with a mischievous streak."),
        PokemonLookalike(66, "Machop", RgbColor(0.55f, 0.50f, 0.50f), FaceShape.OVAL, "Sturdy, strong-jawed, built for action."),
        PokemonLookalike(52, "Meowth", RgbColor(0.85f, 0.80f, 0.60f), FaceShape.OVAL, "Sly, quick-witted, always scheming."),
        PokemonLookalike(54, "Psyduck", RgbColor(0.90f, 0.80f, 0.40f), FaceShape.OVAL, "Perpetually puzzled but lovable."),
        PokemonLookalike(37, "Vulpix", RgbColor(0.90f, 0.60f, 0.40f), FaceShape.OVAL, "Elegant features, fox-like sharpness."),
        PokemonLookalike(133, "Eevee", RgbColor(0.70f, 0.55f, 0.40f), FaceShape.ROUND, "Adaptable, friendly, universally likable."),
        PokemonLookalike(58, "Growlithe", RgbColor(0.85f, 0.50f, 0.25f), FaceShape.OVAL, "Loyal-looking with a strong, defined face."),
        PokemonLookalike(35, "Clefairy", RgbColor(0.95f, 0.75f, 0.80f), FaceShape.ROUND, "Sweet, round-cheeked, charming."),
        PokemonLookalike(69, "Bellsprout", RgbColor(0.45f, 0.70f, 0.35f), FaceShape.LONG, "Long face, tall energy."),
        PokemonLookalike(79, "Slowpoke", RgbColor(0.90f, 0.70f, 0.75f), FaceShape.LONG, "Laid-back with a long, thoughtful expression."),
        PokemonLookalike(78, "Rapidash", RgbColor(0.95f, 0.90f, 0.85f), FaceShape.LONG, "Sleek features, fiery spirit."),
        PokemonLookalike(24, "Arbok", RgbColor(0.55f, 0.35f, 0.60f), FaceShape.LONG, "Sharp, angular, a bit intimidating."),
        PokemonLookalike(149, "Dragonite", RgbColor(0.90f, 0.70f, 0.40f), FaceShape.ROUND, "Big, friendly, huggable energy."),
        PokemonLookalike(131, "Lapras", RgbColor(0.55f, 0.70f, 0.85f), FaceShape.OVAL, "Gentle, wide, calm expression."),
        PokemonLookalike(95, "Onix", RgbColor(0.55f, 0.55f, 0.55f), FaceShape.LONG, "Tall, angular, imposing presence."),
        PokemonLookalike(132, "Ditto", RgbColor(0.80f, 0.70f, 0.85f), FaceShape.ROUND, "Blank-slate cute, blends in anywhere."),
        PokemonLookalike(129, "Magikarp", RgbColor(0.90f, 0.55f, 0.40f), FaceShape.OVAL, "Underestimated at first glance."),
        PokemonLookalike(12, "Butterfree", RgbColor(0.85f, 0.80f, 0.90f), FaceShape.OVAL, "Delicate features, graceful."),
        PokemonLookalike(40, "Wigglytuff", RgbColor(0.95f, 0.80f, 0.85f), FaceShape.ROUND, "Big round cheeks, soft expression."),
        PokemonLookalike(65, "Alakazam", RgbColor(0.90f, 0.80f, 0.35f), FaceShape.LONG, "Sharp-featured, thoughtful, intense stare."),
        PokemonLookalike(76, "Golem", RgbColor(0.50f, 0.42f, 0.35f), FaceShape.ROUND, "Rugged, rock-solid jawline."),
        PokemonLookalike(38, "Ninetales", RgbColor(0.92f, 0.85f, 0.75f), FaceShape.OVAL, "Refined, elegant, fox-sharp features."),
        PokemonLookalike(9, "Blastoise", RgbColor(0.30f, 0.55f, 0.75f), FaceShape.OVAL, "Strong jaw, composed and steady."),
        PokemonLookalike(3, "Venusaur", RgbColor(0.30f, 0.60f, 0.40f), FaceShape.OVAL, "Grounded with a memorable look."),
        PokemonLookalike(6, "Charizard", RgbColor(0.90f, 0.50f, 0.25f), FaceShape.LONG, "Bold, angular, larger-than-life presence."),
        PokemonLookalike(150, "Mewtwo", RgbColor(0.65f, 0.55f, 0.75f), FaceShape.LONG, "Sharp, striking, intense features."),
        PokemonLookalike(151, "Mew", RgbColor(0.95f, 0.75f, 0.80f), FaceShape.ROUND, "Soft, playful, universally cute."),
        PokemonLookalike(197, "Umbreon", RgbColor(0.20f, 0.20f, 0.25f), FaceShape.OVAL, "Sleek dark features, striking accents."),
        PokemonLookalike(196, "Espeon", RgbColor(0.75f, 0.55f, 0.75f), FaceShape.LONG, "Elegant, refined, sharp-eyed."),
        PokemonLookalike(136, "Flareon", RgbColor(0.90f, 0.45f, 0.30f), FaceShape.ROUND, "Warm, fluffy-cheeked energy."),
        PokemonLookalike(134, "Vaporeon", RgbColor(0.45f, 0.65f, 0.85f), FaceShape.OVAL, "Smooth, sleek, calm-water vibes."),
        PokemonLookalike(135, "Jolteon", RgbColor(0.95f, 0.90f, 0.40f), FaceShape.OVAL, "Electric energy, sharp features."),
        PokemonLookalike(255, "Torchic", RgbColor(0.95f, 0.70f, 0.35f), FaceShape.ROUND, "Round-cheeked and cheerful.")
    )
}
