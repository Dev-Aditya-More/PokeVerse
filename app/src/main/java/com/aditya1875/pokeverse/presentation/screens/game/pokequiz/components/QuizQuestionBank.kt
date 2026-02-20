package com.aditya1875.pokeverse.presentation.screens.game.pokequiz.components

object QuizQuestionBank {

    private val allQuestions = listOf(

        QuizQuestion(
            id = 1,
            question = "What type is Pikachu?",
            options = listOf("Electric", "Fire", "Water", "Normal"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.EASY,
            category = QuizCategory.TYPES,
            explanation = "Pikachu is an Electric-type Pokémon, known for its powerful Thunderbolt attack!"
        ),

        QuizQuestion(
            id = 2,
            question = "Which Pokémon evolves into Charizard?",
            options = listOf("Charmander", "Squirtle", "Bulbasaur", "Eevee"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.EASY,
            category = QuizCategory.EVOLUTIONS,
            explanation = "Charmander evolves into Charmeleon at level 16, then Charizard at level 36!"
        ),

        QuizQuestion(
            id = 3,
            question = "What color is Pikachu?",
            options = listOf("Yellow", "Blue", "Red", "Green"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.EASY,
            category = QuizCategory.POKEDEX,
            explanation = "Pikachu is yellow with red cheeks and a lightning bolt-shaped tail!"
        ),

        QuizQuestion(
            id = 4,
            question = "Which Pokémon is known as the 'Mouse Pokémon'?",
            options = listOf("Pikachu", "Squirtle", "Charmander", "Bulbasaur"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.EASY,
            category = QuizCategory.POKEDEX,
            explanation = "Pikachu's Pokédex category is 'Mouse Pokémon'!"
        ),

        QuizQuestion(
            id = 5,
            question = "What type is Squirtle?",
            options = listOf("Water", "Fire", "Grass", "Electric"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.EASY,
            category = QuizCategory.TYPES,
            explanation = "Squirtle is a Water-type, one of the original Kanto starter Pokémon!"
        ),

        QuizQuestion(
            id = 6,
            question = "Which Pokémon is #001 in the National Pokédex?",
            options = listOf("Bulbasaur", "Pikachu", "Charmander", "Squirtle"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.EASY,
            category = QuizCategory.POKEDEX,
            explanation = "Bulbasaur holds the honor of being #001 in the National Pokédex!"
        ),

        QuizQuestion(
            id = 7,
            question = "What does Eevee need to evolve into Vaporeon?",
            options = listOf("Water Stone", "Fire Stone", "Thunder Stone", "Leaf Stone"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.EASY,
            category = QuizCategory.EVOLUTIONS,
            explanation = "Use a Water Stone on Eevee to evolve it into the Water-type Vaporeon!"
        ),

        QuizQuestion(
            id = 8,
            question = "Which region do you start in Pokémon Red/Blue?",
            options = listOf("Kanto", "Johto", "Hoenn", "Sinnoh"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.EASY,
            category = QuizCategory.REGIONS,
            explanation = "Kanto is the first region, home to the original 151 Pokémon!"
        ),

        QuizQuestion(
            id = 9,
            question = "What type is Jigglypuff?",
            options = listOf("Normal/Fairy", "Psychic", "Normal", "Fairy"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.EASY,
            category = QuizCategory.TYPES,
            explanation = "Jigglypuff is Normal/Fairy type as of Generation VI!"
        ),

        QuizQuestion(
            id = 10,
            question = "Which Pokémon can Mega Evolve?",
            options = listOf("Charizard", "Pidgey", "Rattata", "Caterpie"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.EASY,
            category = QuizCategory.EVOLUTIONS,
            explanation = "Charizard has TWO Mega Evolutions: Mega Charizard X and Y!"
        ),

        QuizQuestion(
            id = 11,
            question = "What type is super effective against Fire types?",
            options = listOf("Water", "Grass", "Electric", "Fire"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.EASY,
            category = QuizCategory.TYPES,
            explanation = "Water, Rock, and Ground are all super effective against Fire!"
        ),

        QuizQuestion(
            id = 12,
            question = "What does Magikarp evolve into?",
            options = listOf("Gyarados", "Dragonite", "Lapras", "Seaking"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.EASY,
            category = QuizCategory.EVOLUTIONS,
            explanation = "Magikarp evolves into the powerful Gyarados at level 20!"
        ),

        QuizQuestion(
            id = 13,
            question = "Which type is weak to Psychic attacks?",
            options = listOf("Fighting", "Steel", "Dark", "Fairy"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.EASY,
            category = QuizCategory.TYPES,
            explanation = "Fighting and Poison types are weak to Psychic moves!"
        ),

        QuizQuestion(
            id = 14,
            question = "What is the evolved form of Pikachu?",
            options = listOf("Raichu", "Pichu", "Pachirisu", "Emolga"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.EASY,
            category = QuizCategory.EVOLUTIONS,
            explanation = "Pikachu evolves into Raichu when exposed to a Thunder Stone!"
        ),

        QuizQuestion(
            id = 15,
            question = "Which legendary bird is Ice/Flying type?",
            options = listOf("Articuno", "Zapdos", "Moltres", "Lugia"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.EASY,
            category = QuizCategory.LEGENDARIES,
            explanation = "Articuno is the Ice/Flying legendary bird of Kanto!"
        ),

        QuizQuestion(
            id = 16,
            question = "What type is Grass weak against?",
            options = listOf("Fire", "Water", "Electric", "Normal"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.EASY,
            category = QuizCategory.TYPES,
            explanation = "Grass is weak to Fire, Ice, Poison, Flying, and Bug!"
        ),

        QuizQuestion(
            id = 17,
            question = "Which Pokémon is known for its Thunderbolt attack?",
            options = listOf("Pikachu", "Charmander", "Bulbasaur", "Squirtle"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.EASY,
            category = QuizCategory.MOVES,
            explanation = "Pikachu's signature move is Thunderbolt, a powerful Electric attack!"
        ),

        QuizQuestion(
            id = 18,
            question = "What does Caterpie evolve into?",
            options = listOf("Metapod", "Butterfree", "Weedle", "Kakuna"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.EASY,
            category = QuizCategory.EVOLUTIONS,
            explanation = "Caterpie → Metapod → Butterfree is the evolution line!"
        ),

        QuizQuestion(
            id = 19,
            question = "Which type has an advantage over Water?",
            options = listOf("Electric", "Fire", "Water", "Normal"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.EASY,
            category = QuizCategory.TYPES,
            explanation = "Electric and Grass types are super effective against Water!"
        ),

        QuizQuestion(
            id = 20,
            question = "What is Meowth known for saying?",
            options = listOf("That's right!", "Pika pika!", "Squirtle!", "Bulba!"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.EASY,
            category = QuizCategory.POKEDEX,
            explanation = "Team Rocket's Meowth is famous for saying 'That's right!' in the anime!"
        ),

        QuizQuestion(
            id = 21,
            question = "Which Pokémon is a Ghost/Poison type?",
            options = listOf("Gengar", "Haunter", "Gastly", "All of them"),
            correctAnswerIndex = 3,
            difficulty = QuizDifficulty.EASY,
            category = QuizCategory.TYPES,
            explanation = "Gastly, Haunter, and Gengar are all Ghost/Poison types!"
        ),

        QuizQuestion(
            id = 22,
            question = "What type is Onix?",
            options = listOf("Rock/Ground", "Ground", "Rock", "Steel"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.EASY,
            category = QuizCategory.TYPES,
            explanation = "Onix is Rock/Ground, making it quadruple weak to Water and Grass!"
        ),

        QuizQuestion(
            id = 23,
            question = "Which move heals the user?",
            options = listOf("Recover", "Tackle", "Scratch", "Ember"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.EASY,
            category = QuizCategory.MOVES,
            explanation = "Recover restores 50% of the user's maximum HP!"
        ),

        QuizQuestion(
            id = 24,
            question = "What is the first Gym type in Pokémon Red/Blue?",
            options = listOf("Rock", "Water", "Fire", "Grass"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.EASY,
            category = QuizCategory.REGIONS,
            explanation = "Brock's Pewter City Gym specializes in Rock-type Pokémon!"
        ),

        QuizQuestion(
            id = 25,
            question = "Which Pokémon uses Vine Whip?",
            options = listOf("Bulbasaur", "Charmander", "Squirtle", "Pikachu"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.EASY,
            category = QuizCategory.MOVES,
            explanation = "Vine Whip is a Grass-type move commonly used by Bulbasaur!"
        ),

        QuizQuestion(
            id = 26,
            question = "What type is Snorlax?",
            options = listOf("Normal", "Fighting", "Dark", "Psychic"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.EASY,
            category = QuizCategory.TYPES,
            explanation = "Snorlax is a Normal-type known for sleeping and blocking paths!"
        ),

        QuizQuestion(
            id = 27,
            question = "Which region is Pichu from?",
            options = listOf("Johto", "Kanto", "Hoenn", "Sinnoh"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.EASY,
            category = QuizCategory.REGIONS,
            explanation = "Pichu was introduced in Generation II (Johto region)!"
        ),

        QuizQuestion(
            id = 28,
            question = "What does Poliwag evolve into?",
            options = listOf("Poliwhirl", "Poliwrath", "Politoed", "Golduck"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.EASY,
            category = QuizCategory.EVOLUTIONS,
            explanation = "Poliwag → Poliwhirl → Poliwrath or Politoed!"
        ),

        QuizQuestion(
            id = 29,
            question = "Which type is immune to Ghost moves?",
            options = listOf("Normal", "Fighting", "Psychic", "Dark"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.EASY,
            category = QuizCategory.TYPES,
            explanation = "Normal types are completely immune to Ghost-type attacks!"
        ),

        QuizQuestion(
            id = 30,
            question = "What is Psyduck's type?",
            options = listOf("Water", "Psychic", "Water/Psychic", "Normal"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.EASY,
            category = QuizCategory.TYPES,
            explanation = "Despite its name, Psyduck is pure Water-type!"
        ),

        QuizQuestion(
            id = 31,
            question = "Which legendary is featured on Pokémon Gold?",
            options = listOf("Ho-Oh", "Lugia", "Suicune", "Entei"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.EASY,
            category = QuizCategory.LEGENDARIES,
            explanation = "Ho-Oh graces the cover of Pokémon Gold!"
        ),

        QuizQuestion(
            id = 32,
            question = "What type is Machop?",
            options = listOf("Fighting", "Normal", "Rock", "Ground"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.EASY,
            category = QuizCategory.TYPES,
            explanation = "Machop is a pure Fighting-type Pokémon!"
        ),

        QuizQuestion(
            id = 33,
            question = "Which move puts opponents to sleep?",
            options = listOf("Hypnosis", "Tackle", "Slash", "Bite"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.EASY,
            category = QuizCategory.MOVES,
            explanation = "Hypnosis is a Psychic move that puts the target to sleep!"
        ),

        QuizQuestion(
            id = 34,
            question = "What does Geodude evolve into?",
            options = listOf("Graveler", "Golem", "Onix", "Rhyhorn"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.EASY,
            category = QuizCategory.EVOLUTIONS,
            explanation = "Geodude → Graveler → Golem (trade evolution)!"
        ),

        QuizQuestion(
            id = 35,
            question = "Which type is strong against Dragon?",
            options = listOf("Ice", "Fire", "Water", "Electric"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.EASY,
            category = QuizCategory.TYPES,
            explanation = "Ice, Dragon, and Fairy types are super effective against Dragon!"
        ),

        // ═══════════════════════════════════════════════════════════
        // MEDIUM QUESTIONS (36-70)
        // ═══════════════════════════════════════════════════════════

        QuizQuestion(
            id = 36,
            question = "What is Gyarados's ability?",
            options = listOf("Intimidate", "Swift Swim", "Moxie", "Hyper Cutter"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.MEDIUM,
            category = QuizCategory.ABILITIES,
            explanation = "Intimidate lowers the opponent's Attack stat when Gyarados enters battle!"
        ),

        QuizQuestion(
            id = 37,
            question = "Which type combination is Charizard X?",
            options = listOf("Fire/Dragon", "Fire/Flying", "Fire/Steel", "Dragon/Flying"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.MEDIUM,
            category = QuizCategory.TYPES,
            explanation = "Mega Charizard X becomes Fire/Dragon, while Y stays Fire/Flying!"
        ),

        QuizQuestion(
            id = 38,
            question = "What is the base stat total of Mewtwo?",
            options = listOf("680", "600", "720", "540"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.MEDIUM,
            category = QuizCategory.STATS,
            explanation = "Mewtwo's base stat total is 680, making it one of the strongest!"
        ),

        QuizQuestion(
            id = 39,
            question = "Which ability prevents status conditions?",
            options = listOf("Immunity", "Synchronize", "Shed Skin", "Natural Cure"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.MEDIUM,
            category = QuizCategory.ABILITIES,
            explanation = "Immunity prevents the Pokémon from being poisoned!"
        ),

        QuizQuestion(
            id = 40,
            question = "What is the accuracy of Thunder?",
            options = listOf("70%", "80%", "90%", "100%"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.MEDIUM,
            category = QuizCategory.MOVES,
            explanation = "Thunder has 70% accuracy, but never misses in rain!"
        ),

        QuizQuestion(
            id = 41,
            question = "Which Pokémon has the highest base HP?",
            options = listOf("Blissey", "Chansey", "Snorlax", "Wailord"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.MEDIUM,
            category = QuizCategory.STATS,
            explanation = "Blissey has a massive base HP of 255!"
        ),

        QuizQuestion(
            id = 42,
            question = "What type is Lucario?",
            options = listOf("Fighting/Steel", "Fighting/Psychic", "Steel/Psychic", "Fighting"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.MEDIUM,
            category = QuizCategory.TYPES,
            explanation = "Lucario's unique Fighting/Steel typing gives it great resistances!"
        ),

        QuizQuestion(
            id = 43,
            question = "Which move has the highest base power?",
            options = listOf("Explosion", "Hyper Beam", "Tackle", "Flamethrower"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.MEDIUM,
            category = QuizCategory.MOVES,
            explanation = "Explosion has 250 base power but causes the user to faint!"
        ),

        QuizQuestion(
            id = 44,
            question = "What is Dragonite's hidden ability?",
            options = listOf("Multiscale", "Inner Focus", "Marvel Scale", "Shed Skin"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.MEDIUM,
            category = QuizCategory.ABILITIES,
            explanation = "Multiscale halves damage taken when HP is full!"
        ),

        QuizQuestion(
            id = 45,
            question = "Which Pokémon has the ability Levitate?",
            options = listOf("Bronzong", "Steelix", "Golem", "Rhydon"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.MEDIUM,
            category = QuizCategory.ABILITIES,
            explanation = "Levitate makes the Pokémon immune to Ground-type moves!"
        ),

        QuizQuestion(
            id = 46,
            question = "What is the evolution method for Feebas?",
            options = listOf("High Beauty", "Level 30", "Water Stone", "Trade"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.MEDIUM,
            category = QuizCategory.EVOLUTIONS,
            explanation = "Feebas evolves into Milotic when leveled up with high Beauty!"
        ),

        QuizQuestion(
            id = 47,
            question = "Which type resists Fairy moves?",
            options = listOf("Steel", "Water", "Rock", "Ground"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.MEDIUM,
            category = QuizCategory.TYPES,
            explanation = "Fire, Poison, and Steel resist Fairy-type moves!"
        ),

        QuizQuestion(
            id = 48,
            question = "What is Garchomp's base Speed stat?",
            options = listOf("102", "95", "108", "100"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.MEDIUM,
            category = QuizCategory.STATS,
            explanation = "Garchomp's base 102 Speed makes it a fast pseudo-legendary!"
        ),

        QuizQuestion(
            id = 49,
            question = "Which ability boosts Speed in rain?",
            options = listOf("Swift Swim", "Drizzle", "Rain Dish", "Hydration"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.MEDIUM,
            category = QuizCategory.ABILITIES,
            explanation = "Swift Swim doubles Speed in rain!"
        ),

        QuizQuestion(
            id = 50,
            question = "What is the only type that is super effective against Fairy?",
            options = listOf("Steel", "Poison", "Fire", "Both Steel and Poison"),
            correctAnswerIndex = 3,
            difficulty = QuizDifficulty.MEDIUM,
            category = QuizCategory.TYPES,
            explanation = "Both Steel and Poison are super effective against Fairy!"
        ),

        QuizQuestion(
            id = 51,
            question = "Which Pokémon can learn Fly and Surf?",
            options = listOf("Dragonite", "Charizard", "Gyarados", "All of them"),
            correctAnswerIndex = 3,
            difficulty = QuizDifficulty.MEDIUM,
            category = QuizCategory.MOVES,
            explanation = "All three can learn both HMs, making them versatile!"
        ),

        QuizQuestion(
            id = 52,
            question = "What is Mimikyu's ability?",
            options = listOf("Disguise", "Wonder Guard", "Prankster", "Shadow Shield"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.MEDIUM,
            category = QuizCategory.ABILITIES,
            explanation = "Disguise blocks the first damaging hit it takes!"
        ),

        QuizQuestion(
            id = 53,
            question = "Which Pokémon has the highest base Attack?",
            options = listOf("Mega Mewtwo X", "Rayquaza", "Garchomp", "Dragonite"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.MEDIUM,
            category = QuizCategory.STATS,
            explanation = "Mega Mewtwo X has a base Attack of 190!"
        ),

        QuizQuestion(
            id = 54,
            question = "What type is immune to Electric moves?",
            options = listOf("Ground", "Rock", "Steel", "Flying"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.MEDIUM,
            category = QuizCategory.TYPES,
            explanation = "Ground types are completely immune to Electric attacks!"
        ),

        QuizQuestion(
            id = 55,
            question = "Which ability prevents confusion?",
            options = listOf("Own Tempo", "Clear Body", "Simple", "Oblivious"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.MEDIUM,
            category = QuizCategory.ABILITIES,
            explanation = "Own Tempo prevents the Pokémon from becoming confused!"
        ),

        QuizQuestion(
            id = 56,
            question = "What is Toxapex's type combination?",
            options = listOf("Poison/Water", "Water/Dark", "Poison/Steel", "Water/Steel"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.MEDIUM,
            category = QuizCategory.TYPES,
            explanation = "Toxapex is Poison/Water, making it a defensive powerhouse!"
        ),

        QuizQuestion(
            id = 57,
            question = "Which move has priority?",
            options = listOf("Quick Attack", "Tackle", "Scratch", "Ember"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.MEDIUM,
            category = QuizCategory.MOVES,
            explanation = "Quick Attack has +1 priority, meaning it goes first!"
        ),

        QuizQuestion(
            id = 58,
            question = "What is Rotom-Wash's type?",
            options = listOf("Electric/Water", "Water/Steel", "Electric/Steel", "Water/Ghost"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.MEDIUM,
            category = QuizCategory.TYPES,
            explanation = "Rotom-Wash's Electric/Water typing gives it only one weakness!"
        ),

        QuizQuestion(
            id = 59,
            question = "Which ability increases evasion in a sandstorm?",
            options = listOf("Sand Veil", "Sand Stream", "Sand Rush", "Sand Force"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.MEDIUM,
            category = QuizCategory.ABILITIES,
            explanation = "Sand Veil boosts evasion by 20% in a sandstorm!"
        ),

        QuizQuestion(
            id = 60,
            question = "What is Ferrothorn's ability?",
            options = listOf("Iron Barbs", "Sturdy", "Rough Skin", "Spiky Shield"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.MEDIUM,
            category = QuizCategory.ABILITIES,
            explanation = "Iron Barbs damages attackers that make contact!"
        ),

        QuizQuestion(
            id = 61,
            question = "Which type combination has the most resistances?",
            options = listOf("Steel/Fairy", "Steel/Flying", "Steel/Dragon", "Steel/Psychic"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.MEDIUM,
            category = QuizCategory.TYPES,
            explanation = "Steel/Fairy resists 9 types and is immune to 2!"
        ),

        QuizQuestion(
            id = 62,
            question = "What does Scyther need to evolve into Scizor?",
            options = listOf("Metal Coat + Trade", "Level 30", "High Friendship", "Sun Stone"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.MEDIUM,
            category = QuizCategory.EVOLUTIONS,
            explanation = "Trade Scyther while holding a Metal Coat to get Scizor!"
        ),

        QuizQuestion(
            id = 63,
            question = "Which ability heals HP in rain?",
            options = listOf("Rain Dish", "Swift Swim", "Drizzle", "Hydration"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.MEDIUM,
            category = QuizCategory.ABILITIES,
            explanation = "Rain Dish restores 1/16 of max HP each turn in rain!"
        ),

        QuizQuestion(
            id = 64,
            question = "What is Alakazam's highest base stat?",
            options = listOf("Special Attack", "Speed", "Special Defense", "HP"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.MEDIUM,
            category = QuizCategory.STATS,
            explanation = "Alakazam's Special Attack is 135, making it a special sweeper!"
        ),

        QuizQuestion(
            id = 65,
            question = "Which Pokémon has the ability Huge Power?",
            options = listOf("Azumarill", "Slaking", "Regigigas", "Medicham"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.MEDIUM,
            category = QuizCategory.ABILITIES,
            explanation = "Huge Power doubles the Attack stat!"
        ),

        QuizQuestion(
            id = 66,
            question = "What type is Aegislash in Shield Forme?",
            options = listOf("Steel/Ghost", "Steel/Fighting", "Ghost/Fighting", "Steel"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.MEDIUM,
            category = QuizCategory.TYPES,
            explanation = "Aegislash is Steel/Ghost in both its formes!"
        ),

        QuizQuestion(
            id = 67,
            question = "Which move sets up Stealth Rock?",
            options = listOf("Stealth Rock", "Rock Tomb", "Stone Edge", "Rock Slide"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.MEDIUM,
            category = QuizCategory.MOVES,
            explanation = "Stealth Rock damages switching Pokémon, super effective against Flying!"
        ),

        QuizQuestion(
            id = 68,
            question = "What is Volcarona's type?",
            options = listOf("Bug/Fire", "Fire/Flying", "Bug/Flying", "Fire/Psychic"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.MEDIUM,
            category = QuizCategory.TYPES,
            explanation = "Volcarona's Bug/Fire typing is unique and powerful!"
        ),

        QuizQuestion(
            id = 69,
            question = "Which ability prevents crits?",
            options = listOf("Shell Armor", "Battle Armor", "Lucky Chant", "Both A and B"),
            correctAnswerIndex = 3,
            difficulty = QuizDifficulty.MEDIUM,
            category = QuizCategory.ABILITIES,
            explanation = "Both Shell Armor and Battle Armor prevent critical hits!"
        ),

        QuizQuestion(
            id = 70,
            question = "What is Garchomp's hidden ability?",
            options = listOf("Rough Skin", "Sand Veil", "Sand Force", "Sand Stream"),
            correctAnswerIndex = 2,
            difficulty = QuizDifficulty.MEDIUM,
            category = QuizCategory.ABILITIES,
            explanation = "Sand Force boosts Ground, Rock, and Steel moves in sandstorm!"
        ),

        // ═══════════════════════════════════════════════════════════
        // HARD QUESTIONS (71-100)
        // ═══════════════════════════════════════════════════════════

        QuizQuestion(
            id = 71,
            question = "What is the base power of Seismic Toss?",
            options = listOf("Equals user's level", "100", "80", "Variable"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.HARD,
            category = QuizCategory.MOVES,
            explanation = "Seismic Toss deals damage equal to the user's level!"
        ),

        QuizQuestion(
            id = 72,
            question = "Which ability boosts Sp. Atk in harsh sunlight?",
            options = listOf("Solar Power", "Chlorophyll", "Drought", "Flower Gift"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.HARD,
            category = QuizCategory.ABILITIES,
            explanation = "Solar Power boosts Sp. Atk by 50% but drains HP in sun!"
        ),

        QuizQuestion(
            id = 73,
            question = "What is Heatran's type?",
            options = listOf("Fire/Steel", "Fire/Rock", "Steel/Ground", "Fire/Ground"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.HARD,
            category = QuizCategory.TYPES,
            explanation = "Heatran's unique Fire/Steel typing gives it many resistances!"
        ),

        QuizQuestion(
            id = 74,
            question = "Which move has the highest critical hit ratio?",
            options = listOf("Frost Breath", "Stone Edge", "Slash", "Night Slash"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.HARD,
            category = QuizCategory.MOVES,
            explanation = "Frost Breath ALWAYS lands a critical hit!"
        ),

        QuizQuestion(
            id = 75,
            question = "What is Landorus-T's base Attack stat?",
            options = listOf("145", "135", "130", "150"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.HARD,
            category = QuizCategory.STATS,
            explanation = "Landorus-Therian has a massive base 145 Attack!"
        ),

        QuizQuestion(
            id = 76,
            question = "Which ability prevents secondary effects?",
            options = listOf("Shield Dust", "Inner Focus", "Keen Eye", "Clear Body"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.HARD,
            category = QuizCategory.ABILITIES,
            explanation = "Shield Dust prevents additional effects of attacks!"
        ),

        QuizQuestion(
            id = 77,
            question = "What is Clefable's base stat total?",
            options = listOf("483", "500", "525", "450"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.HARD,
            category = QuizCategory.STATS,
            explanation = "Clefable's balanced 483 BST makes it a great utility Pokémon!"
        ),

        QuizQuestion(
            id = 78,
            question = "Which type combination has the most weaknesses?",
            options = listOf("Rock/Dark", "Grass/Ice", "Psychic/Fighting", "Bug/Grass"),
            correctAnswerIndex = 1,
            difficulty = QuizDifficulty.HARD,
            category = QuizCategory.TYPES,
            explanation = "Grass/Ice has 7 weaknesses including 4x to Fire!"
        ),

        QuizQuestion(
            id = 79,
            question = "What is Kartana's base Attack stat?",
            options = listOf("181", "170", "165", "190"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.HARD,
            category = QuizCategory.STATS,
            explanation = "Kartana has the highest non-Mega base Attack at 181!"
        ),

        QuizQuestion(
            id = 80,
            question = "Which ability changes forme based on HP?",
            options = listOf("Power Construct", "Stance Change", "Shields Down", "Zen Mode"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.HARD,
            category = QuizCategory.ABILITIES,
            explanation = "Power Construct transforms Zygarde at 50% HP!"
        ),

        QuizQuestion(
            id = 81,
            question = "What is the base power of Psyshock?",
            options = listOf("80", "90", "70", "85"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.HARD,
            category = QuizCategory.MOVES,
            explanation = "Psyshock is special but hits the target's Defense stat!"
        ),

        QuizQuestion(
            id = 82,
            question = "Which Pokémon has the ability Regenerator?",
            options = listOf("Toxapex", "Ferrothorn", "Skarmory", "Clefable"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.HARD,
            category = QuizCategory.ABILITIES,
            explanation = "Regenerator heals 1/3 max HP when switching out!"
        ),

        QuizQuestion(
            id = 83,
            question = "What is Mega Salamence's ability?",
            options = listOf("Aerilate", "Intimidate", "Moxie", "Sheer Force"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.HARD,
            category = QuizCategory.ABILITIES,
            explanation = "Aerilate turns Normal moves into Flying with 1.2x boost!"
        ),

        QuizQuestion(
            id = 84,
            question = "Which move ignores abilities?",
            options = listOf("Mold Breaker", "Sunsteel Strike", "Moongeist Beam", "All of them"),
            correctAnswerIndex = 3,
            difficulty = QuizDifficulty.HARD,
            category = QuizCategory.MOVES,
            explanation = "All three ignore abilities when attacking!"
        ),

        QuizQuestion(
            id = 85,
            question = "What is Zacian's base Attack with Crowned Sword?",
            options = listOf("170", "150", "165", "180"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.HARD,
            category = QuizCategory.STATS,
            explanation = "Zacian-Crowned has a ridiculous base 170 Attack!"
        ),

        QuizQuestion(
            id = 86,
            question = "Which ability heals allies' status?",
            options = listOf("Healer", "Natural Cure", "Immunity", "Shed Skin"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.HARD,
            category = QuizCategory.ABILITIES,
            explanation = "Healer has a 30% chance to cure allies' status each turn!"
        ),

        QuizQuestion(
            id = 87,
            question = "What is the accuracy of Zap Cannon?",
            options = listOf("50%", "60%", "70%", "80%"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.HARD,
            category = QuizCategory.MOVES,
            explanation = "Zap Cannon has 120 power and 50% accuracy, always paralyzes!"
        ),

        QuizQuestion(
            id = 88,
            question = "Which type is Arceus when holding a Draco Plate?",
            options = listOf("Dragon", "Normal", "Steel", "Fairy"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.HARD,
            category = QuizCategory.TYPES,
            explanation = "Arceus's type changes to match its held plate!"
        ),

        QuizQuestion(
            id = 89,
            question = "What is Tapu Lele's base Sp. Atk stat?",
            options = listOf("130", "125", "135", "140"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.HARD,
            category = QuizCategory.STATS,
            explanation = "Tapu Lele's 130 Sp. Atk makes it a Psychic terrain sweeper!"
        ),

        QuizQuestion(
            id = 90,
            question = "Which ability prevents all entry hazards?",
            options = listOf("Magic Bounce", "Defiant", "Competitive", "Clear Body"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.HARD,
            category = QuizCategory.ABILITIES,
            explanation = "Magic Bounce reflects hazards and status moves back!"
        ),

        QuizQuestion(
            id = 91,
            question = "What is Mega Rayquaza's base stat total?",
            options = listOf("780", "720", "760", "800"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.HARD,
            category = QuizCategory.STATS,
            explanation = "Mega Rayquaza's 780 BST is tied for highest ever!"
        ),

        QuizQuestion(
            id = 92,
            question = "Which move has perfect accuracy in rain?",
            options = listOf("Thunder", "Hurricane", "Blizzard", "Both A and B"),
            correctAnswerIndex = 3,
            difficulty = QuizDifficulty.HARD,
            category = QuizCategory.MOVES,
            explanation = "Thunder and Hurricane never miss in rain!"
        ),

        QuizQuestion(
            id = 93,
            question = "What is Slowbro's hidden ability?",
            options = listOf("Regenerator", "Own Tempo", "Oblivious", "Shell Armor"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.HARD,
            category = QuizCategory.ABILITIES,
            explanation = "Regenerator makes Slowbro an incredible defensive pivot!"
        ),

        QuizQuestion(
            id = 94,
            question = "Which ability increases Speed after KOing?",
            options = listOf("Moxie", "Guts", "Beast Boost", "Defiant"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.HARD,
            category = QuizCategory.ABILITIES,
            explanation = "Moxie boosts Attack; Weak Armor boosts Speed!"
        ),

        QuizQuestion(
            id = 95,
            question = "What is Celesteela's BST?",
            options = listOf("570", "580", "600", "550"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.HARD,
            category = QuizCategory.STATS,
            explanation = "As an Ultra Beast, Celesteela has 570 BST!"
        ),

        QuizQuestion(
            id = 96,
            question = "Which ability changes type based on terrain?",
            options = listOf("Mimicry", "Protean", "Color Change", "Forecast"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.HARD,
            category = QuizCategory.ABILITIES,
            explanation = "Mimicry changes type to match active terrain!"
        ),

        QuizQuestion(
            id = 97,
            question = "What is the base power of Meteor Mash?",
            options = listOf("90", "80", "100", "85"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.HARD,
            category = QuizCategory.MOVES,
            explanation = "Meteor Mash is 90 power with 90% accuracy!"
        ),

        QuizQuestion(
            id = 98,
            question = "Which Pokémon has the ability Prism Armor?",
            options = listOf("Necrozma", "Solgaleo", "Lunala", "Dusk Mane"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.HARD,
            category = QuizCategory.ABILITIES,
            explanation = "Prism Armor reduces super effective damage by 25%!"
        ),

        QuizQuestion(
            id = 99,
            question = "What is Pheromosa's base Speed?",
            options = listOf("151", "145", "160", "140"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.HARD,
            category = QuizCategory.STATS,
            explanation = "Pheromosa is the fastest Ultra Beast with 151 Speed!"
        ),

        QuizQuestion(
            id = 100,
            question = "Which move sets up both screens at once?",
            options = listOf("Aurora Veil", "Light Screen", "Reflect", "Safeguard"),
            correctAnswerIndex = 0,
            difficulty = QuizDifficulty.HARD,
            category = QuizCategory.MOVES,
            explanation = "Aurora Veil sets Reflect + Light Screen in hail/snow!"
        )
    )

    fun getQuestionsByDifficulty(difficulty: QuizDifficulty): List<QuizQuestion> {
        return allQuestions
            .filter { it.difficulty == difficulty }
            .shuffled()
            .take(difficulty.questionCount)
    }

    fun getAllQuestions() = allQuestions
}