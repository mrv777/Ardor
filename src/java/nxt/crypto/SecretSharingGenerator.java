/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2020 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of this software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

package nxt.crypto;

import nxt.util.Convert;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SecretSharingGenerator {

    enum Version {
        /**
         * Type of secret sharing that encodes a custom string.
         */
        CUSTOM_PASSPHRASE {
            @Override
            String numberToSecret(BigInteger secret) {
                return Convert.toString(Convert.parseHexString(secret.toString(16)));
            }

            @Override
            BigInteger secretToNumber(String customPassphrase) {
                return new BigInteger(Convert.toHexString(Convert.toBytes(customPassphrase)), 16);
            }
        },
        /**
         * Type of secret sharing that encodes a Nxt/Ardor legacy 12-word passphrase.
         */
        LEGACY_WORDS {
            @Override
            String numberToSecret(BigInteger secret) {
                return String.join(" ", from128bit(secret));
            }

            @Override
            BigInteger secretToNumber(String legacyPassphrase) {
                return to128bit(legacyPassphrase.split(" "));
            }

            /**
             * Given array of secret phrase words, compose a 128+ bits integer based on the words offset in the words list.
             * This code is compatible with the secret phrase generation in passphrasegenerator.js
             * @param secretPhraseWords the secret phrase words
             * @return compact numeric representation
             */
            private BigInteger to128bit(String[] secretPhraseWords) {
                BigInteger n128 = BigInteger.ZERO;
                for (int i = 0; i < secretPhraseWords.length / 3; i++) {
                    n128 = n128.add(new BigInteger("" + getSignedInt(secretPhraseWords[3 * i], secretPhraseWords[3 * i + 1], secretPhraseWords[3 * i + 2])));
                    if (i == secretPhraseWords.length / 3 - 1) {
                        break;
                    }
                    n128 = n128.shiftLeft(Integer.SIZE);
                }
                return n128;
            }

            private long getSignedInt(String w1, String w2, String w3) {
                return getSignedInt(LEGACY_WORDS_MAP.get(w1), LEGACY_WORDS_MAP.get(w2), LEGACY_WORDS_MAP.get(w3));
            }

            private long getSignedInt(int w1, int w2, int w3) {
                int n = LEGACY_PHRASE_WORDS.length;

                // The calculation below reverses the calculation performed in the from128bit method which is compatible
                // with the old Ardor 2.2.6 passphrasegenerator.js
                // Note that normal % modulo operator won't work here since it returns negative values and this calculation
                // must use positive values for compatibility with Javascript code.
                // Also note the & at the end to convert int to signed int represented as long
                return (w1 + Math.floorMod(w2 - w1, n) * n + Math.floorMod(w3 - w2, n) * n * n) & 0x00000000ffffffffL;
            }

            private String[] from128bit(BigInteger n128orig) {
                String[] words = new String[12];
                String[] allSecretPhraseWords = LEGACY_PHRASE_WORDS;
                int n = allSecretPhraseWords.length;
                long w1, w2, w3;
                BigInteger n128 = new BigInteger(n128orig.toString());
                for (int i = 0; i < 4; i++) {
                    long x = n128.intValue() & 0x00000000ffffffffL;
                    n128 = n128.shiftRight(Integer.SIZE);
                    w1 = x % n;
                    w2 = (((x / n)) + w1) % n;
                    w3 = (((((x / n)) / n)) + w2) % n;
                    if (w2 < 0 || w2 >= n || w3 < 0 || w3 >= n) {
                        return NON_STANDARD_SECRET;
                    }
                    int index = 3 * (4 - i - 1);
                    words[index] = allSecretPhraseWords[(int) w1];
                    words[index + 1] = allSecretPhraseWords[(int) w2];
                    words[index + 2] = allSecretPhraseWords[(int) w3];
                }
                if (n128.compareTo(BigInteger.ZERO) > 0) {
                    throw new IllegalStateException(String.format("number %s has more than 128 bit", n128orig));
                }
                return words;
            }
        },
        /**
         * Type of secret sharing that encodes a BIP39 mnemonic using the english word list.
         */
        BIP39_EN_WORDS {
            @Override
            String numberToSecret(BigInteger secret) {
                return String.join(" ", numberToMnemonic(secret));
            }

            @Override
            BigInteger secretToNumber(String mnemonic) {
                return mnemonicToNumber(mnemonic.split(" "));
            }

            /**
             * Generate a compact secret integer from a BIP39 mnemonic. We can't use just the entropy as the secret
             * because we also need to encode the number of bits or words (we choose the latter as it's more compact).
             *
             * @param mnemonic the BIP39 mnemonic
             * @return an integer with the entropy and a byte with the number of words of the original mnemonic
             */
            private BigInteger mnemonicToNumber(String[] mnemonic) {
                BigInteger entropy = BIP39.mnemonicToEntropy(mnemonic);
                return entropy.shiftLeft(8).add(BigInteger.valueOf(mnemonic.length));
            }

            /**
             * Restore the original BIP39 mnemonic from a BIP39 entropy and a byte for the number of words.
             *
             * @param bits the bits of the encoded secret (entropy + number of words)
             * @return the original BIP39 mnemonic
             */
            private String[] numberToMnemonic(BigInteger bits) {
                int numberOfWords = bits.and(BigInteger.valueOf(255)).intValueExact();
                return BIP39.entropyToMnemonic(bits.shiftRight(8), BIP39.getEntropyBits(numberOfWords));
            }

        },
        /**
         * Type of secret sharing that encodes a private key (256 bit integer).
         */
        PRIVATE_KEY {
            @Override
            String numberToSecret(BigInteger secret) {
                return Convert.bigIntegerToHexString(secret, 64);
            }

            @Override
            BigInteger secretToNumber(String hexString) {
                return new BigInteger(hexString, 16);
            }
        };

        /**
         * Convert a secret represented as number back to a a encoded secret (passphrase, mnemonic, hex...).
         *
         * @param secret the secret number
         * @return the original string
         */
        abstract String numberToSecret(BigInteger secret);

        /**
         * Convert a encoded secret (passphrase, mnemonic, hex...) into a number.
         *
         * @param secretPhrase the passphrase
         * @return the secret represented as number
         */
        abstract BigInteger secretToNumber(String secretPhrase);

        /**
         * Given a list of words for a passphrase guesses the corresponding version that can handle it.
         *
         * @param words the passphrase words
         * @return the best matching version
         */
        static Version detectWordListVersion(String[] words) {
            int length = words.length;
            if (BIP39.isValidMnemonic(words)) {
                return BIP39_EN_WORDS;
            }
            if (length == 12 && Arrays.stream(words).allMatch(LEGACY_WORDS_MAP::containsKey)) {
                return LEGACY_WORDS;
            }
            return CUSTOM_PASSPHRASE;
        }

        /**
         * Parses a version from a string containing a version integer.
         *
         * @param version the version integer as a string
         * @return the corresponding version
         */
        static Version parseVersionString(String version) {
            int v = Integer.parseInt(version);
            if (v < 0 || v > values().length) {
                throw new IllegalArgumentException("Unsupported version: " + version);
            }
            return values()[v];
        }
    }

    static final BigInteger PRIME_4096_BIT = new BigInteger(
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
    static final BigInteger PRIME_384_BIT = new BigInteger("830856716641269388050926147210" +
            "378437007763661599988974204336" +
            "741171904442622602400099072063" +
            "84693584652377753448639527"
    );
    static final BigInteger PRIME_192_BIT = new BigInteger("14976407493557531125525728362448106789840013430353915016137");

    private static final String[] LEGACY_PHRASE_WORDS = {"like", "just", "love", "know", "never", "want", "time",
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
            "strain", "sunset", "suspend", "sympathy", "thigh", "throne", "total", "unseen", "weapon", "weary"};

    private static final Map<String, Integer> LEGACY_WORDS_MAP = IntStream.range(0, LEGACY_PHRASE_WORDS.length).boxed().collect(Collectors.toMap(i -> LEGACY_PHRASE_WORDS[i], i -> i));

    private static final String[] NON_STANDARD_SECRET = new String[0]; // Signal that the secret is not composed from 12 words selected from the WORDS array

    private static SecretSharing getSecretSharingEngine() {
        return new SimpleShamirSecretSharing();
    }

    /**
     * Given a secretPhrase split it into totalPieces pieces where each minPieces of them are enough to reproduce the secret.
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
     * @param declaredModePrime the modulo prime used for secret sharing calculations. Specify BigInteger.ZERO to calculate automatically
     * @return the secret pieces
     */
    public static String[] split(String secretPhrase, int totalPieces, int minPieces, BigInteger declaredModePrime) {
        String[] words = secretPhrase.split(" ");
        Version version = Version.detectWordListVersion(words);
        BigInteger secretInteger = version.secretToNumber(secretPhrase);
        return split(secretInteger, totalPieces, minPieces, declaredModePrime, version);
    }

    /**
     * Given a secret byte array split it into totalPieces pieces where each minPieces of them are enough to reproduce the secret.
     *
     * Pieces are returned with a piece number followed by : and the piece content.
     * The piece number is required when reproducing the secret.
     *
     * @param privateKey the secret byte array
     * @param totalPieces the number of generated pieces
     * @param minPieces the number of pieces needed to reproduce the secret phrase
     * @param declaredModePrime the modulo prime used for secret sharing calculations. Specify BigInteger.ZERO to calculate automatically
     * @return the secret pieces
     */
    public static String[] split(byte[] privateKey, int totalPieces, int minPieces, BigInteger declaredModePrime) {
        Version version = Version.PRIVATE_KEY;
        BigInteger secretInteger = version.secretToNumber(Convert.toHexString(privateKey));
        return split(secretInteger, totalPieces, minPieces, declaredModePrime, version);
    }

    private static String[] split(BigInteger secretInteger, int totalPieces, int minPieces, BigInteger declaredModePrime, Version version) {
        if (minPieces <= 1 || minPieces > totalPieces) {
            throw new IllegalArgumentException(String.format("Illegal number of minimum pieces %d, must be between 2 and %d", minPieces, totalPieces));
        }

        BigInteger modPrime = getModPrime(declaredModePrime, secretInteger);

        // Split the number into pieces
        Random random = Crypto.getSecureRandom();
        SecretShare[] splitSecretOutput = getSecretSharingEngine().split(secretInteger, minPieces, totalPieces, modPrime, random);

        // Convert the pieces back to readable strings prefixed by piece number.
        // We encode the secret data using hex string even if the original secret phrase was composed of 12 words
        String prefix = String.format("%d:%d:%d:%d:%s:", version.ordinal(), random.nextInt(), totalPieces, minPieces, declaredModePrime.toString());
        return Arrays.stream(splitSecretOutput).map(piece -> prefix + piece.getX() + ":" + Convert.toHexString(piece.getShare().toByteArray())).toArray(String[]::new);
    }

    /**
     * Given minPieces pieces out of totalPieces of a privateKey, combine the pieces into the original.
     * Pieces are formatted as [version]:[split id]:[total pieces]:[minimum pieces]:[prime field size or zero]:[piece number]:[piece content]
     *
     * @param encodedSecretPhrasePieces the pieces of the private key formatted as specified above
     * @return the reproduced private key
     */
    public static byte[] combinePrivateKey(String[] encodedSecretPhrasePieces) {
        if (!isPrivateKeySecret(encodedSecretPhrasePieces)) {
            throw new IllegalArgumentException("Expecting secret pieces generated from a private key");
        }
        return Convert.parseHexString(combineImpl(encodedSecretPhrasePieces));
    }

    /**
     * Given minPieces pieces out of totalPieces of a secretPhrase, combine the pieces into the original phrase.*
     * Pieces are formatted as [version]:[split id]:[total pieces]:[minimum pieces]:[prime field size or zero]:[piece number]:[piece content]
     *
     * @param encodedSecretPhrasePieces the pieces of the secret phrase formatted as specified above
     * @return the reproduced secret phrase
     */
    public static String combine(String[] encodedSecretPhrasePieces) {
        if (isPrivateKeySecret(encodedSecretPhrasePieces)) {
            throw new IllegalArgumentException("Expecting secret pieces for anything but a private key");
        }
        return combineImpl(encodedSecretPhrasePieces);
    }

    private static String combineImpl(String[] encodedSecretPhrasePieces) {
        String[] secretPhrasePieces = new String[encodedSecretPhrasePieces.length];

        // Parse the combination parameters from the first piece
        String[] tokens = encodedSecretPhrasePieces[0].split(":", 6);
        if (tokens.length < 6) {
            throw new IllegalArgumentException("Wrong piece format, should be v:id:n:k:p:#:data");
        }
        Version version = Version.parseVersionString(tokens[0]);
        int id = Integer.parseInt(tokens[1]);
        int totalPieces = Integer.parseInt(tokens[2]);
        int minPieces = Integer.parseInt(tokens[3]);
        BigInteger modPrime = new BigInteger(tokens[4]);
        secretPhrasePieces[0] = tokens[5];

        // Make sure all other pieces contains the same parameters
        for (int i = 1; i < encodedSecretPhrasePieces.length; i++) {
            tokens = encodedSecretPhrasePieces[i].split(":", 6);
            if (tokens.length != 6) {
                throw new IllegalArgumentException("Wrong piece format, should be n:k:p:#:data");
            }
            try {
                if (version != Version.parseVersionString(tokens[0])) {
                    throw new IllegalArgumentException("Version differs between pieces");
                } else if (id != Integer.parseInt(tokens[1])) {
                    throw new IllegalArgumentException("Id differs between pieces");
                } else if (totalPieces != Integer.parseInt(tokens[2])) {
                    throw new IllegalArgumentException("Total number of shares differs between pieces");
                } else if (minPieces != Integer.parseInt(tokens[3])) {
                    throw new IllegalArgumentException("Minimum number of shares differs between pieces");
                } else if (!modPrime.equals(new BigInteger(tokens[4]))) {
                    throw new IllegalArgumentException("Modulo prime differs between pieces");
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Wrong piece numeric values, should be v:id:n:k:p:#:data");
            }
            secretPhrasePieces[i] = tokens[5];
        }
        if (secretPhrasePieces.length < minPieces) {
            throw new IllegalArgumentException(String.format("Need %d pieces to combine the original secret, only %d unique piece(s) available", minPieces, secretPhrasePieces.length));
        }
        return combineImpl(secretPhrasePieces, modPrime, version);
    }
    /**
     * Given minPieces pieces out of totalPieces of a secretPhrase, combine the pieces into the original phrase.*
     * Pieces should formatted with a piece number followed by ':' followed by the piece content.
     *
     * @param secretPhrasePieces the pieces of the secret phrase formatted as [index]:[data]
     * @param modPrime the prime number representing the finite field size used by the secret sharing polynomial. Specify BigInteger.ZERO to calculate automatically
     * @param version the piece version
     * @return the reproduced secret phrase
     */
    private static String combineImpl(String[] secretPhrasePieces, BigInteger modPrime, Version version) {
        List<SecretShare> secretShares = Arrays.stream(secretPhrasePieces).map(string -> {
            String[] split = string.split(":", 2);
            if (split.length != 2) {
                throw new IllegalArgumentException("shared secret not formatted as #:data");
            }
            return new SecretShare(Integer.parseInt(split[0]), new BigInteger(split[1], 16));
        }).collect(Collectors.toList());

        // Get the optimal mod prime size
        BigInteger maxShare = secretShares.stream().map(SecretShare::getShare).max(Comparator.naturalOrder()).orElse(BigInteger.ZERO);
        modPrime = getModPrime(modPrime, maxShare);

        // Combine the secret by converting the indexed element to shares (pieces in the expected format)
        BigInteger secretInteger = getSecretSharingEngine().combine(secretShares.toArray(new SecretShare[0]), modPrime);
        return version.numberToSecret(secretInteger);
    }

    public static boolean isPrivateKeySecret(String[] encodedSecretPhrasePieces) {
        String[] tokens = encodedSecretPhrasePieces[0].split(":", 2);
        Version version = Version.parseVersionString(tokens[0]);
        for (int i = 1; i < encodedSecretPhrasePieces.length; i++) {
            tokens = encodedSecretPhrasePieces[0].split(":", 2);
            if (version != Version.parseVersionString(tokens[0])) {
                throw new IllegalArgumentException("Version differs between pieces");
            }
        }
        return version == Version.PRIVATE_KEY;
    }

    /**
     * Given a secret phrase calculate the modulo prime finite field size which is sufficiently large to split the
     * shared secrets.
     *
     * The specific encoding (version) used is guessed trying to find the best one.
     *
     * @param secretPhrase the secret phrase
     * @return the prime number representing the finite field size
     */
    public static BigInteger getModPrime(String secretPhrase) {
        Version version = Version.detectWordListVersion(secretPhrase.split(" "));
        BigInteger secretInteger = version.secretToNumber(secretPhrase);
        return getModPrime(BigInteger.ZERO, secretInteger);
    }

    /**
     * Given a private key calculate the modulo prime finite field size which is sufficiently large to split the key.
     *
     * @param privateKey the private key
     * @return the prime number representing the finite field size
     */
    public static BigInteger getModPrime(byte[] privateKey) {
        Version version = Version.PRIVATE_KEY;
        BigInteger secretInteger = version.secretToNumber(Convert.toHexString(privateKey));
        return getModPrime(BigInteger.ZERO, secretInteger);
    }

    private static BigInteger getModPrime(BigInteger modPrime, BigInteger secretInteger) {
        // Determine the mod prime size
        if (!BigInteger.ZERO.equals(modPrime)) {
            if (secretInteger.compareTo(modPrime) >= 0) {
                throw new IllegalArgumentException("Secret cannot be larger than modulus.  " + "Secret=" + secretInteger + " Modulus=" + modPrime);
            }
            return modPrime;
        }
        return getModPrimeForSecret(secretInteger);
    }

    private static BigInteger getModPrimeForSecret(BigInteger secret) {
        if (secret.compareTo(SecretSharingGenerator.PRIME_192_BIT) < 0) {
            return SecretSharingGenerator.PRIME_192_BIT;
        } else if (secret.compareTo(SecretSharingGenerator.PRIME_384_BIT) < 0) {
            return SecretSharingGenerator.PRIME_384_BIT;
        } else if (secret.compareTo(SecretSharingGenerator.PRIME_4096_BIT) < 0) {
            return SecretSharingGenerator.PRIME_4096_BIT;
        } else {
            // if you make it here, you are 4000+ bits big and this call is going to be really expensive
            throw new IllegalStateException("Cannot split secrets of more than 4024 bit");
        }
    }

}
