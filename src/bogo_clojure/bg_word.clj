(ns bogo-clojure.bg-word
  (:require [clojure.string :as string]
            [bogo-clojure.bg-char :refer :all]))

;;; This namespace includes function to process single **VALID**
;;; vietnamese words. Any words serving as parameter of functions in
;;; this namespace have to be a valid vietnamese word or be able to
;;; develop to a valid vietnamese word.

(def CONSONANTS #{"b" "c" "ch" "d" "g" "gh" "gi" "h" "k"
                    "kh" "l" "m" "n" "ng" "ngh" "nh" "p" "ph"
                    "qu" "r" "s" "t" "th" "tr" "v" "x" "đ" ""})

(def TERMINAL_CONSONANTS #{"c" "ch" "m" "n" "ng" "nh" "p" "t" ""})

(def VOWELS #{ "a" "ai" "ao" "au" "ay" "e" "eo" "i" "ia" "iu" "iê"
               "ie" "iêu" "ieu" "o" "oa" "oai" "oao" "oay" "oe" "oeo"
               "oi" "oo" "oă" "u" "ua" "ui" "uy" "uya" "uyu" "uyê"
               "uye" "uâ" "uây" "uay" "uê" "ue" "uô" "uôi" "uoi"
               "uơ" "y" "yê" "ye" "yêu" "yeu" "â" "âu" "ây" "ê" "êu"
               "ô" "ôi" "ă" "ơ" "ơi" "ư" "ưa" "ưi" "ưu" "uu" "ươ" "uo"
               "ươi" "ươu" "uou" ""})

(def TERMINAL_VOWELS #{ "ai" "ao" "au" "ay" "eo" "ia" "iu" "iêu"
                        "ieu" "oai" "oao" "oay" "oeo" "oi" "ua" "ui"
                        "uya" "uyu" "uây" "uay" "uôi" "uoi" "uơ" "uo"
                        "yêu" "yeu" "âu" "ây" "êu" "eu" "ôi" "ơi" "ưa"
                        "ưi" "ưu" "uu" "ươi" "ươu" "uou" ""})

(defn word-structure-when-no-accent
  [comps]
  (if ( = "" (first comps) (second comps))
   (list (last comps) (second comps) "")
    comps))

(defn word-structure*
  "Split a word into 3 components: first-consonant, vowel,
  last-consonant * last-consonant: the longest substring of consonants
  at the end of given word * vowel: the longest substring of vowel next
  to the last-consonant * first-consonant: the remaining characters.
  Return value is a vector with the form:
  [first-consonant vowel last-consonant]"
  [word]
  (let [rword (reverse word)]
    (mapv string/reverse
          (word-structure-when-no-accent
           (reduce (fn [comps char]
                     (let [c  (str char)]
                       (cond
                        (and (single-consonant? c) (= (comps 1) ""))
                        ["" "" (str (comps 2) c)]
                        (and (single-vowel? c) (= (comps 0) ""))
                        ["" (str (comps 1) c) (comps 2)]
                        :else [(str (comps 0) c) (comps 1) (comps 2)]
                        )
                       ))
                   ["" "" ""]
                   rword)))))

(defn word-structure
  "Similar to word-structure* but this function is more appropriate
  when processing qu and gi"
  [word]
  (let [comps (word-structure* word)
        last-comp0 (str (last (nth comps 0)))
        first-comp1 (str (first (nth comps 1)))]
    (if (or (and (= "q" (string/lower-case last-comp0))
                 (= "u" (string/lower-case first-comp1)))
            (and (= "g" (string/lower-case last-comp0))
                 (= "i" (string/lower-case first-comp1))
                 (> (count (nth comps 1)) 1)))
      [(str (nth comps 0) first-comp1) (subs (nth comps 1) 1) (nth comps 2)]
      comps)))

(defn normalize
  "Lower case and remove any accent"
  [word]
  (reduce #(str %1 (string/lower-case (remove-accent-char %2)))
          ""
          word))

(defn valid-word-qu-gi?
  [comps]
  (let [[first-consonant vowel last-consonant] comps]
       (cond
        (and (= first-consonant "qu") (= (first vowel) \u )) false
        (and (= first-consonant "gi") (= (first vowel) \i)) false
        :else true)))

(defn valid-word?
  "Return true if the given word is a valid vietnamese words or is
  extendable to a valid vietnamese word"
  [word]
  (let [comps (word-structure (normalize word))
        [first-consonant vowel last-consonant] comps]
    (if (and (contains? CONSONANTS first-consonant)
             (contains? VOWELS vowel)
             (contains? TERMINAL_CONSONANTS last-consonant)
             (valid-word-qu-gi? comps))
      (if (contains? TERMINAL_VOWELS vowel)
        (= "" last-consonant)
        (case last-consonant
          "ch" (contains? #{"a" "ê" "uê" "i" "uy" "oa"} vowel)
          "c" (not (= "ơ" vowel))
          true))
      false)))

(defn remove-accent-word
  "Remove accent from word"
  [word]
  (string/join (mapv remove-accent-char word))
  )

(defn accent->word
  "Add accent to a valid word. Always keep in mind that the input has
  to be a valid word otherwise it causes error."
  [word accent]
  (let [comps (word-structure word)
        vowel (remove-accent-word (comps 1))
        nvowel (normalize vowel)
        vowel-size (count vowel)
        position-ơ-ê (+ 1 (.indexOf nvowel "ơ") (.indexOf nvowel "ê"))]
    (cond
     (empty? vowel) word
     (> position-ơ-ê -1)
     (string/join [(comps 0) (subs vowel 0 position-ơ-ê)
                   (accent->char (nth vowel position-ơ-ê) accent)
                   (subs vowel (inc position-ơ-ê))
                   (comps 2)])
     (or (= 3 vowel-size) (and (= 2 vowel-size) (not (empty? (comps 2)))))
     (string/join [(comps 0)
                   (nth vowel 0)
                   (accent->char (nth vowel 1) accent)
                   (subs vowel 2)
                   (comps 2)])
     :else (string/join [ (comps 0)
                          (accent->char (nth vowel 0) accent)
                          (subs vowel 1)
                          (comps 2)]))))

(defn mark->word*
  [word mark]
  (string/join (mapv (fn [c] (mark->char c mark))
                     word)))

(defn refine-mark->word
  "Refine mark adding in case vowel is ươu or ưu"
  [word mark]
  (let [comps (vec (word-structure word))
        vowel (comps 1)
        nvowel (normalize vowel)
        vowel-size (count vowel)]
    (if (or (= "ưư" nvowel) (= "ươư" nvowel))
      (string/join [(comps 0)
                    (subs vowel 0 (dec vowel-size))
                    (remove-mark-char (nth vowel (dec vowel-size)))
                    (comps 2)])
      word)))


(defn mark->word
  "Add mark to a valid word. Always keep in mind that the input has to
  be a valid word otherwise it causes error."
  [word mark]
  (refine-mark->word (string/join (mapv (fn [c] (mark->char c mark))
                                       word))
                    mark)
  )

(defn get-last-word
  "Get the longest valid word lying at the end of the given string"
  [astring]
  (if (valid-word? astring)
    astring
    (get-last-word (subs astring 1))
    ))

(defn grammar-split-word
  "Split the string into 2 parts, the later part is the longest valid word
  at the end of the string, the former part is the remaining."
  [astring]
  (let [last-word (get-last-word astring)
        position (- (count astring) (count last-word))
        first-word (subs astring 0 position)]
    [first-word last-word]))

(defn accent->string
  "Add accent to a string"
  [astring accent]
  (let [[first-word last-word] (grammar-split-word astring)]
    (string/join [first-word (accent->word last-word accent)])))

(defn mark->string
  "Add mark to a string"
  [astring mark]
  (let [[first-word last-word] (grammar-split-word astring)]
    (string/join [first-word (mark->word last-word mark)])))

(defn word->accent
  "Determin the accent in the word. Obviously, one word has one accent which is
  added into its vowel"
  [word]
  (reduce #(if (not= :none (char->accent %2))
             (char->accent %2)
             %1)
          :none
          word))

(defn vowel-of-word->mark
  "Determine the mark applied to the vowel part of the word"
  [word]
  (reduce #(if (not= :nomark (char->mark %2))
             (char->mark %2)
             %1)
          :nomark
          (second (word-structure word))))

(defn refine-accent
  [word]
  (let [accent (word->accent word)]
    (if (not= "" (get-last-word word)) ; Solve the problem when there are some
                                        ; deliminating characters at the end of
                                        ; the word.
      (accent->word word accent)
      word)))

(defn refine-mark
  [word]
  (let [mark (vowel-of-word->mark word)]
    (if (not= :nomark mark)
      (mark->word word mark)
      word)))

(defn refine-word
  "When some accents, marks or new char is applied to the original word, the position
  of marks or accents can be wrong. This function fix these mistakes to give a vietnamese
  valid word"
  [word]
  (refine-accent (refine-mark word)))
