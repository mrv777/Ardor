/******************************************************************************
 * Copyright © 2016-2020 Jelurida IP B.V.                                     *
 *                                                                            *
 * See the LICENSE.txt file at the top-level directory of this distribution   *
 * for licensing information.                                                 *
 *                                                                            *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,*
 * no part of this software, including this file, may be copied, modified,    *
 * propagated, or distributed except according to the terms contained in the  *
 * LICENSE.txt file.                                                          *
 *                                                                            *
 * Removal or modification of this copyright notice is prohibited.            *
 *                                                                            *
 ******************************************************************************/

var sss = function () {

    var PRIME_4096_BIT = bigInt(
        "1671022210261044010706804337146599012127" +
        "9427984758140486147735732543262527544919" +
        "3095812289909599609334542417074310282054" +
        "0780117501097269771621177740562184444713" +
        "5311624699359973445785442150139493030849" +
        "1201896951396220211014303634039307573549" +
        "4951338587994892653929285926514054477984" +
        "1897745831487644537568464106991023630108" +
        "6045751504900830441750495932712549251755" +
        "0884842714308894440025555839788342744866" +
        "7101368958164663781091806630951947745404" +
        "9899622319436016030246615841346729868014" +
        "9869334160881652755341231281231973786191" +
        "0590928243420749213395009469338508019541" +
        "0958855418900088036159728065975165578015" +
        "3079187511387238090409461192977321170936" +
        "6081401737953645348323163171237010704282" +
        "8481068031277612787461827099245660019965" +
        "4423851454616735972464821439378482870833" +
        "7709298145449348366148476664877596527269" +
        "1765522730435723049823184958030880339674" +
        "1433100452606317504985611860713079871716" +
        "8809146278034477061142090096734446658190" +
        "8273334857030516871663995504285034522155" +
        "7158160427604895839673593745279150722839" +
        "3997083495197879290548002853265127569910" +
        "9306488129210915495451479419727501586051" +
        "1232507931203905482587057398637416125459" +
        "0876872367709717423642369650017374448020" +
        "8386154750356267714638641781056467325078" +
        "08534977443900875333446450467047221"
    );

    var PRIME_384_BIT = bigInt("830856716641269388050926147210" +
        "378437007763661599988974204336" +
        "741171904442622602400099072063" +
        "84693584652377753448639527"
    );

    var PRIME_192_BIT = bigInt("14976407493557531125525728362448106789840013430353915016137");

    const LEGACY_PHRASE_WORDS = ["like", "just", "love", "know", "never", "want", "time",
        "out", "there", "make", "look", "eye", "down", "only", "think", "heart", "back", "then", "into", "about",
        "more", "away", "still", "them", "take", "thing", "even", "through", "long", "always", "world", "too",
        "friend", "tell", "try", "hand", "thought", "over", "here", "other", "need", "smile", "again", "much",
        "cry", "been", "night", "ever", "little", "said", "end", "some", "those", "around", "mind", "people",
        "girl", "leave", "dream", "left", "turn", "myself", "give", "nothing", "really", "off", "before",
        "something", "find", "walk", "wish", "good", "once", "place", "ask", "stop", "keep", "watch", "seem",
        "everything", "wait", "got", "yet", "made", "remember", "start", "alone", "run", "hope", "maybe", "believe",
        "body", "hate", "after", "close", "talk", "stand", "own", "each", "hurt", "help", "home", "god", "soul",
        "new", "many", "two", "inside", "should", "true", "first", "fear", "mean", "better", "play", "another",
        "gone", "change", "use", "wonder", "someone", "hair", "cold", "open", "best", "any", "behind", "happen",
        "water", "dark", "laugh", "stay", "forever", "name", "work", "show", "sky", "break", "came", "deep",
        "door", "put", "black", "together", "upon", "happy", "such", "great", "white", "matter", "fill", "past",
        "please", "burn", "cause", "enough", "touch", "moment", "soon", "voice", "scream", "anything", "stare",
        "sound", "red", "everyone", "hide", "kiss", "truth", "death", "beautiful", "mine", "blood", "broken",
        "very", "pass", "next", "forget", "tree", "wrong", "air", "mother", "understand", "lip", "hit", "wall",
        "memory", "sleep", "free", "high", "realize", "school", "might", "skin", "sweet", "perfect", "blue", "kill",
        "breath", "dance", "against", "fly", "between", "grow", "strong", "under", "listen", "bring", "sometimes",
        "speak", "pull", "person", "become", "family", "begin", "ground", "real", "small", "father", "sure", "feet",
        "rest", "young", "finally", "land", "across", "today", "different", "guy", "line", "fire", "reason",
        "reach", "second", "slowly", "write", "eat", "smell", "mouth", "step", "learn", "three", "floor", "promise",
        "breathe", "darkness", "push", "earth", "guess", "save", "song", "above", "along", "both", "color", "house",
        "almost", "sorry", "anymore", "brother", "okay", "dear", "game", "fade", "already", "apart", "warm",
        "beauty", "heard", "notice", "question", "shine", "began", "piece", "whole", "shadow", "secret", "street",
        "within", "finger", "point", "morning", "whisper", "child", "moon", "green", "story", "glass", "kid",
        "silence", "since", "soft", "yourself", "empty", "shall", "angel", "answer", "baby", "bright", "dad",
        "path", "worry", "hour", "drop", "follow", "power", "war", "half", "flow", "heaven", "act", "chance",
        "fact", "least", "tired", "children", "near", "quite", "afraid", "rise", "sea", "taste", "window", "cover",
        "nice", "trust", "lot", "sad", "cool", "force", "peace", "return", "blind", "easy", "ready", "roll", "rose",
        "drive", "held", "music", "beneath", "hang", "mom", "paint", "emotion", "quiet", "clear", "cloud", "few",
        "pretty", "bird", "outside", "paper", "picture", "front", "rock", "simple", "anyone", "meant", "reality",
        "road", "sense", "waste", "bit", "leaf", "thank", "happiness", "meet", "men", "smoke", "truly", "decide",
        "self", "age", "book", "form", "alive", "carry", "escape", "damn", "instead", "able", "ice", "minute",
        "throw", "catch", "leg", "ring", "course", "goodbye", "lead", "poem", "sick", "corner", "desire", "known",
        "problem", "remind", "shoulder", "suppose", "toward", "wave", "drink", "jump", "woman", "pretend", "sister",
        "week", "human", "joy", "crack", "grey", "pray", "surprise", "dry", "knee", "less", "search", "bleed",
        "caught", "clean", "embrace", "future", "king", "son", "sorrow", "chest", "hug", "remain", "sat", "worth",
        "blow", "daddy", "final", "parent", "tight", "also", "create", "lonely", "safe", "cross", "dress", "evil",
        "silent", "bone", "fate", "perhaps", "anger", "class", "scar", "snow", "tiny", "tonight", "continue",
        "control", "dog", "edge", "mirror", "month", "suddenly", "comfort", "given", "loud", "quickly", "gaze",
        "plan", "rush", "stone", "town", "battle", "ignore", "spirit", "stood", "stupid", "yours", "brown", "build",
        "dust", "hey", "kept", "pay", "phone", "twist", "although", "ball", "beyond", "hidden", "nose", "taken",
        "fail", "float", "pure", "somehow", "wash", "wrap", "angry", "cheek", "creature", "forgotten", "heat",
        "rip", "single", "space", "special", "weak", "whatever", "yell", "anyway", "blame", "job", "choose",
        "country", "curse", "drift", "echo", "figure", "grew", "laughter", "neck", "suffer", "worse", "yeah",
        "disappear", "foot", "forward", "knife", "mess", "somewhere", "stomach", "storm", "beg", "idea", "lift",
        "offer", "breeze", "field", "five", "often", "simply", "stuck", "win", "allow", "confuse", "enjoy",
        "except", "flower", "seek", "strength", "calm", "grin", "gun", "heavy", "hill", "large", "ocean", "shoe",
        "sigh", "straight", "summer", "tongue", "accept", "crazy", "everyday", "exist", "grass", "mistake", "sent",
        "shut", "surround", "table", "ache", "brain", "destroy", "heal", "nature", "shout", "sign", "stain",
        "choice", "doubt", "glance", "glow", "mountain", "queen", "stranger", "throat", "tomorrow", "city",
        "either", "fish", "flame", "rather", "shape", "spin", "spread", "ash", "distance", "finish", "image",
        "imagine", "important", "nobody", "shatter", "warmth", "became", "feed", "flesh", "funny", "lust", "shirt",
        "trouble", "yellow", "attention", "bare", "bite", "money", "protect", "amaze", "appear", "born", "choke",
        "completely", "daughter", "fresh", "friendship", "gentle", "probably", "six", "deserve", "expect", "grab",
        "middle", "nightmare", "river", "thousand", "weight", "worst", "wound", "barely", "bottle", "cream",
        "regret", "relationship", "stick", "test", "crush", "endless", "fault", "itself", "rule", "spill", "art",
        "circle", "join", "kick", "mask", "master", "passion", "quick", "raise", "smooth", "unless", "wander",
        "actually", "broke", "chair", "deal", "favorite", "gift", "note", "number", "sweat", "box", "chill",
        "clothes", "lady", "mark", "park", "poor", "sadness", "tie", "animal", "belong", "brush", "consume", "dawn",
        "forest", "innocent", "pen", "pride", "stream", "thick", "clay", "complete", "count", "draw", "faith",
        "press", "silver", "struggle", "surface", "taught", "teach", "wet", "bless", "chase", "climb", "enter",
        "letter", "melt", "metal", "movie", "stretch", "swing", "vision", "wife", "beside", "crash", "forgot",
        "guide", "haunt", "joke", "knock", "plant", "pour", "prove", "reveal", "steal", "stuff", "trip", "wood",
        "wrist", "bother", "bottom", "crawl", "crowd", "fix", "forgive", "frown", "grace", "loose", "lucky",
        "party", "release", "surely", "survive", "teacher", "gently", "grip", "speed", "suicide", "travel", "treat",
        "vein", "written", "cage", "chain", "conversation", "date", "enemy", "however", "interest", "million",
        "page", "pink", "proud", "sway", "themselves", "winter", "church", "cruel", "cup", "demon", "experience",
        "freedom", "pair", "pop", "purpose", "respect", "shoot", "softly", "state", "strange", "bar", "birth",
        "curl", "dirt", "excuse", "lord", "lovely", "monster", "order", "pack", "pants", "pool", "scene", "seven",
        "shame", "slide", "ugly", "among", "blade", "blonde", "closet", "creek", "deny", "drug", "eternity", "gain",
        "grade", "handle", "key", "linger", "pale", "prepare", "swallow", "swim", "tremble", "wheel", "won", "cast",
        "cigarette", "claim", "college", "direction", "dirty", "gather", "ghost", "hundred", "loss", "lung",
        "orange", "present", "swear", "swirl", "twice", "wild", "bitter", "blanket", "doctor", "everywhere",
        "flash", "grown", "knowledge", "numb", "pressure", "radio", "repeat", "ruin", "spend", "unknown", "buy",
        "clock", "devil", "early", "false", "fantasy", "pound", "precious", "refuse", "sheet", "teeth", "welcome",
        "add", "ahead", "block", "bury", "caress", "content", "depth", "despite", "distant", "marry", "purple",
        "threw", "whenever", "bomb", "dull", "easily", "grasp", "hospital", "innocence", "normal", "receive",
        "reply", "rhyme", "shade", "someday", "sword", "toe", "visit", "asleep", "bought", "center", "consider",
        "flat", "hero", "history", "ink", "insane", "muscle", "mystery", "pocket", "reflection", "shove",
        "silently", "smart", "soldier", "spot", "stress", "train", "type", "view", "whether", "bus", "energy",
        "explain", "holy", "hunger", "inch", "magic", "mix", "noise", "nowhere", "prayer", "presence", "shock",
        "snap", "spider", "study", "thunder", "trail", "admit", "agree", "bag", "bang", "bound", "butterfly",
        "cute", "exactly", "explode", "familiar", "fold", "further", "pierce", "reflect", "scent", "selfish",
        "sharp", "sink", "spring", "stumble", "universe", "weep", "women", "wonderful", "action", "ancient",
        "attempt", "avoid", "birthday", "branch", "chocolate", "core", "depress", "drunk", "especially", "focus",
        "fruit", "honest", "match", "palm", "perfectly", "pillow", "pity", "poison", "roar", "shift", "slightly",
        "thump", "truck", "tune", "twenty", "unable", "wipe", "wrote", "coat", "constant", "dinner", "drove",
        "egg", "eternal", "flight", "flood", "frame", "freak", "gasp", "glad", "hollow", "motion", "peer",
        "plastic", "root", "screen", "season", "sting", "strike", "team", "unlike", "victim", "volume", "warn",
        "weird", "attack", "await", "awake", "built", "charm", "crave", "despair", "fought", "grant", "grief",
        "horse", "limit", "message", "ripple", "sanity", "scatter", "serve", "split", "string", "trick", "annoy",
        "blur", "boat", "brave", "clearly", "cling", "connect", "fist", "forth", "imagination", "iron", "jock",
        "judge", "lesson", "milk", "misery", "nail", "naked", "ourselves", "poet", "possible", "princess", "sail",
        "size", "snake", "society", "stroke", "torture", "toss", "trace", "wise", "bloom", "bullet", "cell",
        "check", "cost", "darling", "during", "footstep", "fragile", "hallway", "hardly", "horizon", "invisible",
        "journey", "midnight", "mud", "nod", "pause", "relax", "shiver", "sudden", "value", "youth", "abuse",
        "admire", "blink", "breast", "bruise", "constantly", "couple", "creep", "curve", "difference", "dumb",
        "emptiness", "gotta", "honor", "plain", "planet", "recall", "rub", "ship", "slam", "soar", "somebody",
        "tightly", "weather", "adore", "approach", "bond", "bread", "burst", "candle", "coffee", "cousin", "crime",
        "desert", "flutter", "frozen", "grand", "heel", "hello", "language", "level", "movement", "pleasure",
        "powerful", "random", "rhythm", "settle", "silly", "slap", "sort", "spoken", "steel", "threaten", "tumble",
        "upset", "aside", "awkward", "bee", "blank", "board", "button", "card", "carefully", "complain", "crap",
        "deeply", "discover", "drag", "dread", "effort", "entire", "fairy", "giant", "gotten", "greet", "illusion",
        "jeans", "leap", "liquid", "march", "mend", "nervous", "nine", "replace", "rope", "spine", "stole",
        "terror", "accident", "apple", "balance", "boom", "childhood", "collect", "demand", "depression",
        "eventually", "faint", "glare", "goal", "group", "honey", "kitchen", "laid", "limb", "machine", "mere",
        "mold", "murder", "nerve", "painful", "poetry", "prince", "rabbit", "shelter", "shore", "shower", "soothe",
        "stair", "steady", "sunlight", "tangle", "tease", "treasure", "uncle", "begun", "bliss", "canvas", "cheer",
        "claw", "clutch", "commit", "crimson", "crystal", "delight", "doll", "existence", "express", "fog",
        "football", "gay", "goose", "guard", "hatred", "illuminate", "mass", "math", "mourn", "rich", "rough",
        "skip", "stir", "student", "style", "support", "thorn", "tough", "yard", "yearn", "yesterday", "advice",
        "appreciate", "autumn", "bank", "beam", "bowl", "capture", "carve", "collapse", "confusion", "creation",
        "dove", "feather", "girlfriend", "glory", "government", "harsh", "hop", "inner", "loser", "moonlight",
        "neighbor", "neither", "peach", "pig", "praise", "screw", "shield", "shimmer", "sneak", "stab", "subject",
        "throughout", "thrown", "tower", "twirl", "wow", "army", "arrive", "bathroom", "bump", "cease", "cookie",
        "couch", "courage", "dim", "guilt", "howl", "hum", "husband", "insult", "led", "lunch", "mock", "mostly",
        "natural", "nearly", "needle", "nerd", "peaceful", "perfection", "pile", "price", "remove", "roam",
        "sanctuary", "serious", "shiny", "shook", "sob", "stolen", "tap", "vain", "void", "warrior", "wrinkle",
        "affection", "apologize", "blossom", "bounce", "bridge", "cheap", "crumble", "decision", "descend",
        "desperately", "dig", "dot", "flip", "frighten", "heartbeat", "huge", "lazy", "lick", "odd", "opinion",
        "process", "puzzle", "quietly", "retreat", "score", "sentence", "separate", "situation", "skill", "soak",
        "square", "stray", "taint", "task", "tide", "underneath", "veil", "whistle", "anywhere", "bedroom", "bid",
        "bloody", "burden", "careful", "compare", "concern", "curtain", "decay", "defeat", "describe", "double",
        "dreamer", "driver", "dwell", "evening", "flare", "flicker", "grandma", "guitar", "harm", "horrible",
        "hungry", "indeed", "lace", "melody", "monkey", "nation", "object", "obviously", "rainbow", "salt",
        "scratch", "shown", "shy", "stage", "stun", "third", "tickle", "useless", "weakness", "worship",
        "worthless", "afternoon", "beard", "boyfriend", "bubble", "busy", "certain", "chin", "concrete", "desk",
        "diamond", "doom", "drawn", "due", "felicity", "freeze", "frost", "garden", "glide", "harmony", "hopefully",
        "hunt", "jealous", "lightning", "mama", "mercy", "peel", "physical", "position", "pulse", "punch", "quit",
        "rant", "respond", "salty", "sane", "satisfy", "savior", "sheep", "slept", "social", "sport", "tuck",
        "utter", "valley", "wolf", "aim", "alas", "alter", "arrow", "awaken", "beaten", "belief", "brand",
        "ceiling", "cheese", "clue", "confidence", "connection", "daily", "disguise", "eager", "erase", "essence",
        "everytime", "expression", "fan", "flag", "flirt", "foul", "fur", "giggle", "glorious", "ignorance", "law",
        "lifeless", "measure", "mighty", "muse", "north", "opposite", "paradise", "patience", "patient", "pencil",
        "petal", "plate", "ponder", "possibly", "practice", "slice", "spell", "stock", "strife", "strip",
        "suffocate", "suit", "tender", "tool", "trade", "velvet", "verse", "waist", "witch", "aunt", "bench",
        "bold", "cap", "certainly", "click", "companion", "creator", "dart", "delicate", "determine", "dish",
        "dragon", "drama", "drum", "dude", "everybody", "feast", "forehead", "former", "fright", "fully", "gas",
        "hook", "hurl", "invite", "juice", "manage", "moral", "possess", "raw", "rebel", "royal", "scale", "scary",
        "several", "slight", "stubborn", "swell", "talent", "tea", "terrible", "thread", "torment", "trickle",
        "usually", "vast", "violence", "weave", "acid", "agony", "ashamed", "awe", "belly", "blend", "blush",
        "character", "cheat", "common", "company", "coward", "creak", "danger", "deadly", "defense", "define",
        "depend", "desperate", "destination", "dew", "duck", "dusty", "embarrass", "engine", "example", "explore",
        "foe", "freely", "frustrate", "generation", "glove", "guilty", "health", "hurry", "idiot", "impossible",
        "inhale", "jaw", "kingdom", "mention", "mist", "moan", "mumble", "mutter", "observe", "ode", "pathetic",
        "pattern", "pie", "prefer", "puff", "rape", "rare", "revenge", "rude", "scrape", "spiral", "squeeze",
        "strain", "sunset", "suspend", "sympathy", "thigh", "throne", "total", "unseen", "weapon", "weary"];

    const LEGACY_WORDS_MAP = {};
    LEGACY_PHRASE_WORDS.forEach((word,index) => LEGACY_WORDS_MAP[word] = index);

    const VERSIONS = {
        CUSTOM_PASSPHRASE: {
            number: 0,
            numberToSecret: function(number) {
                try {
                    return converters.hexStringToString(number.toString(16));
                } catch (e) {
                    throw new Error("cannot_combine_pieces");
                }
            },
            secretToNumber: function(passphrase) {
                return bigInt(converters.stringToHexString(passphrase), 16);
            }
        },
        LEGACY_WORDS: {
            number: 1,
            numberToSecret: function(number) {
                const n = LEGACY_PHRASE_WORDS.length;
                const words = new Array(12);
                const bitmask = bigInt("ffffffff", 16);
                for (let i=0; i < 4; i++    ) {
                    const x = number.and(bitmask).toJSNumber();
                    const w1 = x % n;
                    const w2 = (((x / n) >> 0) + w1) % n;
                    const w3 = (((((x / n) >> 0) / n) >> 0) + w2) % n;
                    const index = 3 * (4 - i - 1);
                    words[index] = LEGACY_PHRASE_WORDS[w1];
                    words[index + 1] = LEGACY_PHRASE_WORDS[w2];
                    words[index + 2] = LEGACY_PHRASE_WORDS[w3];
                    number = number.shiftRight(32);
                }
                return words.join(" ");
            },
            // Given a secret phrase, compose a 128+ bits integer based on the words offset in the legacy words list.
            secretToNumber: (function(){
                function getWordsOffset(w1, w2, w3) {
                    return getOffset(LEGACY_WORDS_MAP[w1], LEGACY_WORDS_MAP[w2], LEGACY_WORDS_MAP[w3]);
                }

                function getOffset(w1, w2, w3) {
                    const n = LEGACY_PHRASE_WORDS.length;

                    // The calculation below reverses the calculation performed in the from128bit method which is compatible with generateMnemonic()
                    // Note that normal % modulo operator won't work here since it returns negative values and this calculation must use positive values
                    // for compatibility with Javascript code. Also note the & at the end to convert int to signed int represented as long
                    return (w1 + mod(w2 - w1, n) * n + mod(w3 - w2, n) * n * n);
                }

                /**
                 * Calculate positive mod
                 * @param n number
                 * @param m number
                 * @returns the positive n%m even if n is negative
                 */
                function mod(n, m) {
                    return ((n % m) + m) % m;
                }

                return function(legacyPassphrase) {
                    const secretPhraseWords = legacyPassphrase.split(" ");
                    let n128 = bigInt.zero;
                    for (let i = 0; i < secretPhraseWords.length / 3; i++) {
                        n128 = n128.add(bigInt(getWordsOffset(secretPhraseWords[3 * i], secretPhraseWords[3 * i + 1], secretPhraseWords[3 * i + 2])));
                        if (i == secretPhraseWords.length / 3 - 1) {
                            break;
                        }
                        n128 = n128.shiftLeft(32);
                    }
                    return n128;
                }
            })()
        },
        BIP39_EN_WORDS: {
            number: 2,
            numberToSecret: function(number) {
                const numberOfWords = number.and(255).toJSNumber();
                const hex = number.shiftRight(8).toString(16).padStart(numberOfWords * 11 * 32 / 33 / 4, "0");
                return BIP39.entropyToMnemonic(converters.hexStringToByteArray(hex));
            },
            secretToNumber: function(mnemonic) {
                return bigInt(BIP39.mnemonicToEntropy(mnemonic), 16).shiftLeft(8).add(mnemonic.split(' ').length);
            }
        },
        PRIVATE_KEY: {
            number: 3,
            numberToSecret: function(number) {
                return number.toString(16).padStart(64, "0");
            },
            secretToNumber: function(privateKey) {
                return bigInt(privateKey, 16);
            }
        },
        // Given a list of words for a passphrase guesses the corresponding version that can handle it.
        detectWordListVersion: function(words) {
            if (BIP39.isValidMnemonic(words)) {
                return VERSIONS.BIP39_EN_WORDS;
            }
            if (words.length && words.length == 12 && words.every(word => LEGACY_WORDS_MAP[word] !== undefined)) {
                return VERSIONS.LEGACY_WORDS;
            }
            return VERSIONS.CUSTOM_PASSPHRASE;
        },
        // Parses a version from a string containing a version integer.
        parseVersionString: function(version) {
            switch (version) {
                case '0': return VERSIONS.CUSTOM_PASSPHRASE;
                case '1': return VERSIONS.LEGACY_WORDS;
                case '2': return VERSIONS.BIP39_EN_WORDS;
                case '3': return VERSIONS.PRIVATE_KEY;
                default : throw new Error('unsupported_piece_version');
            }
        }
    };

    /**
     * Given a secret, split it into "available" shares where providing "needed" shares is enough to reproduce the secret.
     * All calculations are performed mod p, where p is a large prime number.
     *
     * This is equivalent to the Java back-end nxt.crypto.SimpleShamirSecretSharing#split
     *
     * @param secret (bigInt) the secret
     * @param needed the number of shares needed to reproduce it
     * @param available the total number of shares
     * @param prime the prime number
     * @return the secret shares
     */
    function split(secret, needed, available, prime) {
        if (secret.isZero()) {
            throw new Error('Secret zero is not allowed');
        }
        // Create a polynomial of degree representing the number of needed pieces
        var coeff = new Array(needed);
        coeff[0] = secret; // our secret is encoded in the polynomial free term

        // The rest of the coefficients are selected randomly as integers mod p and adjusted to the secret size
        for (var i = 1; i < needed; i++) {
            coeff[i] = bigInt.randBetween(0, prime).mod(secret);
        }

        // Clearly the value of this polynomial at x=0 is our secret
        // We generate the shares by running x from 1 to the number of available shares calculating the polynomial value
        // mod p at each point
        var shares = [];
        for (var x = 1; x <= available; x++) {
            var accum = bigInt(secret);
            for (var exp = 1; exp < needed; exp++) {
                accum = bigIntMod(accum.add(bigIntMod(coeff[exp].multiply(bigInt(x).pow(exp)), prime)), prime);
            }
            shares.push({ x: x, share: accum });
        }

        // The resulting points represent the secret shares
        return shares;
    }

    /**
     * Given the needed number of shares or more, reproduce the original polynomial and extract the secret from its free
     * term. All calculations are performed mod p, where p is a large prime number
     *
     * This is equivalent to the Java back-end nxt.crypto.SimpleShamirSecretSharing#combine
     *
     * @param shares the shares represention points over the polynomial
     * @param prime the prime number
     * @return the original secret reproduced from the free term of the polynomial which passes through these points
     */
    function combine(shares, prime) {
        // An optimized approach to using Lagrange polynomials to find L(0) (the free term)
        // See https://en.wikipedia.org/wiki/Shamir%27s_Secret_Sharing "Computationally Efficient Approach"
        var accum = bigInt.zero;
        for (var formula = 0; formula < shares.length; formula++) {
            var numerator = bigInt.one;
            var denominator = bigInt.one;
            for (var count = 0; count < shares.length; count++) {
                if (formula == count) {
                    continue; // If not the same value
                }

                var startposition = shares[formula].x;
                var nextposition = shares[count].x;

                numerator = bigIntMod(numerator.multiply(bigInt(nextposition).negate()), prime); // (numerator * -nextposition) % prime;
                denominator = bigIntMod(denominator.multiply(bigInt(startposition - nextposition)), prime); // (denominator * (startposition - nextposition)) % prime;
            }
            var value = shares[formula].share;
            var tmp = bigInt(value).multiply(numerator).multiply(denominator.modInv(prime));
            accum = bigIntMod(prime.add(accum).add(tmp), prime); //  (prime + accum + (value * numerator * modInverse(denominator))) % prime;
        }
        return accum;
    }

    /**
     * Given a secretPhrase split it into totalPieces pieces where each minPieces of them are enough to reproduce
     * the secret.
     * If the secret phrase a is valid BIP39 mnemonic using the english wordlist, it is converted to the corresponding
     * entropy between 128 and 256 bits and a byte to store the number of words on the original mnemonic.
     * If the secret phrase is 12 words from the legacy list, the secret is converted into 128 bit number which is very
     * compact.
     * Otherwise the secret phrase is converted to bytes using UTF8 then to a BigInteger.
     * Every one of these cases is codified into the version integer of the resulting pieces.
     *
     * Pieces are returned with a piece number followed by : and the piece content.
     * The piece number is required when reproducing the secret.
     *
     * @param secretPhrase the secret phrase
     * @param totalPieces the number of generated pieces
     * @param minPieces the number of pieces needed to reproduce the secret phrase
     * @param declaredModPrime the modulo prime used for secret sharing calculations or zero to calculate automatically
     * @return the secret pieces
     */
    function splitPhrase(secretPhrase, totalPieces, minPieces, declaredModPrime) {
        const version = VERSIONS.detectWordListVersion(secretPhrase.split(" "));
        const secretInteger = version.secretToNumber(secretPhrase);
        return encodeSecretIntegerIntoPieces(secretInteger, totalPieces, minPieces, declaredModPrime, version.number)
    }

    /**
     * Given a secret byte array split it into totalPieces pieces where each minPieces of them are enough to reproduce
     * the secret.
     *
     * Pieces are returned with a piece number followed by : and the piece content.
     * The piece number is required when reproducing the secret.
     *
     * @param privateKey the secret byte array
     * @param totalPieces the number of generated pieces
     * @param minPieces the number of pieces needed to reproduce the secret phrase
     * @param declaredModPrime the modulo prime used for secret sharing calculations or zero to calculate automatically
     * @return the secret pieces
     */
    function splitPrivateKey(privateKey, totalPieces, minPieces, declaredModPrime) {
        const version = VERSIONS.PRIVATE_KEY;
        const secretInteger = version.secretToNumber(privateKey);
        return encodeSecretIntegerIntoPieces(secretInteger, totalPieces, minPieces, declaredModPrime, version.number);
    }

    function encodeSecretIntegerIntoPieces(secretInteger, totalPieces, minPieces, declaredModPrime, versionNumber) {
        if (minPieces <= 1 || minPieces > totalPieces) {
            throw new Error("Illegal number of minimum pieces " + minPieces + ", must be between 2 and " + totalPieces);
        }

        if (declaredModPrime === undefined) {
            declaredModPrime = bigInt.zero;
        }
        const modPrime = getModPrime(declaredModPrime, secretInteger);

        // Split the number into pieces
        const splitSecretOutput = split(secretInteger, minPieces, totalPieces, modPrime);

        // Convert the pieces back to readable strings prefixed by piece number.
        // We encode the secret data using hex string even if the original secret phrase was composed of 12 words
        const prefix = "" + versionNumber + ":" +
            Math.floor(Math.random() * NRS.constants.MAX_INT_JAVA) + ":" +
            totalPieces + ":" +
            minPieces + ":" +
            declaredModPrime + ":";
        const pieces = new Array(splitSecretOutput.length);
        for (let i=0; i<splitSecretOutput.length; i++) {
            pieces[i] = prefix + splitSecretOutput[i].x + ":" + splitSecretOutput[i].share.toString(16);
        }
        return pieces;
    }

    /**
     * Given minPieces pieces out of totalPieces of a secretPhrase, combine the pieces into the original phrase.*
     * Pieces are formatted as [version]:[split id]:[total pieces]:[minimum pieces]:[prime field size or zero]:[piece number]:[piece content]
     *
     * @param encodedSecretPhrasePieces the pieces of the secret phrase formatted as specified above
     * @return the reproduced secret phrase
     */
    function combineSecret(encodedSecretPhrasePieces) {
        var secretPhrasePieces = new Array(encodedSecretPhrasePieces.length);

        // Parse the combination parameters from the first piece
        var tokens = encodedSecretPhrasePieces[0].split(":", 7);
        if (tokens.length < 7) {
            throw new Error("wrong_piece_format");
        }
        const version = VERSIONS.parseVersionString(tokens[0]);
        var id = parseInt(tokens[1]);
        var totalPieces = parseInt(tokens[2]);
        var minPieces = parseInt(tokens[3]);
        var modPrime = bigInt(tokens[4]);
        secretPhrasePieces[0] = tokens[5] + ":" + tokens[6];

        // Make sure all other pieces contains the same parameters
        for (let i = 1; i < encodedSecretPhrasePieces.length; i++) {
            tokens = encodedSecretPhrasePieces[i].split(":", 7);
            if (tokens.length != 7) {
                throw new Error("wrong_piece_format");
            } else if (version !== VERSIONS.parseVersionString(tokens[0])) {
                throw new Error("version_differs_between_pieces");
            } else if (id != parseInt(tokens[1])) {
                throw new Error("id_differs_between_pieces");
            } else if (totalPieces != parseInt(tokens[2])) {
                throw new Error("total_differs_between_pieces");
            } else if (minPieces != parseInt(tokens[3])) {
                throw new Error("min_differs_between_pieces");
            } else if (!modPrime.equals(bigInt(tokens[4]))) {
                throw new Error("prime_differs_between_pieces");
            }
            secretPhrasePieces[i] = tokens[5] + ":" + tokens[6];
        }
        if (secretPhrasePieces.length < minPieces) {
            throw new Error("not_enough_pieces");
        }

        var secretShares = [];
        var maxShare = bigInt.zero;
        for (let i=0; i<secretPhrasePieces.length; i++) {
            var split = secretPhrasePieces[i].split(":", 2);
            if (split.length != 2) {
                throw new Error("shared_secret_format");
            }
            var share = bigInt(split[1], 16);
            if (share.compareTo(maxShare) > 0) {
                maxShare = share;
            }
            secretShares.push({x: split[0], share: share });
        }

        // Get the optimal mod prime size
        modPrime = getModPrime(modPrime, maxShare);

        // Combine the secret by converting the indexed element to shares (pieces in the expected format)
        var secretInteger = combine(secretShares, modPrime);
        return version.numberToSecret(secretInteger);
    }

    function isPrivateKeySecret(encodedSecretPhrasePieces) {
        const tokens = encodedSecretPhrasePieces[0].split(':', 2);
        const version = VERSIONS.parseVersionString(tokens[0]);
        if (!encodedSecretPhrasePieces.every(piece => piece.split(':', 2)[0] === tokens[0])) {
            throw new Error('version_differs_between_pieces');
        }
        return version === VERSIONS.PRIVATE_KEY;
    }

    /**
     * Calculate positive mod
     * @param n bigInt number
     * @param m bigInt number
     * @returns the positive n%m even if n is negative
     */
    function bigIntMod(n, m) {
        return (n.mod(m).add(m)).mod(m);
    }

    /**
     * Given a secret phrase calculate the modulo prime finite field size which is sufficiently large to split the
     * shared secrets
     * @param modPrime the modulo prime
     * @param secretInteger the secret integer
     * @return the prime number representing the finite field size
     */
    function getModPrime(modPrime, secretInteger) {
        // Determine the mod prime size
        if (!bigInt.zero.equals(modPrime)) {
            if (secretInteger.compareTo(modPrime) >= 0) {
                throw new Error("secret_larger_than_prime");
            }
            return modPrime;
        }
        return getModPrimeForSecret(secretInteger);
    }

    function getModPrimeForSecret(secret) {
        if (secret.compareTo(PRIME_192_BIT) < 0) {
            return PRIME_192_BIT;
        } else if (secret.compareTo(PRIME_384_BIT) < 0) {
            return PRIME_384_BIT;
        } else if (secret.compareTo(PRIME_4096_BIT) < 0) {
            return PRIME_4096_BIT;
        } else {
            // if you make it here, you are 4000+ bits big and this call is going to be really expensive
            throw new Error("secret_too_long");
        }
    }

    return {
        // public API
        splitPhrase : splitPhrase,
        splitPrivateKey : splitPrivateKey,
        combineSecret : combineSecret,
        isPrivateKeySecret: isPrivateKeySecret,

        // exported for unit tests
        split: split,
        combine: combine,
        VERSIONS: VERSIONS,
        PRIME_4096_BIT: PRIME_4096_BIT,
        PRIME_384_BIT: PRIME_384_BIT,
        PRIME_192_BIT: PRIME_192_BIT
    };
}();

if (isNode) {
    // noinspection JSUnresolvedVariable
    module.exports = sss;
}