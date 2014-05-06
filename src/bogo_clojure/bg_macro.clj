(ns bogo-clojure.bg-macro
  (:gen-class)
  (:require [clojure.string :as string]
            [bogo-clojure.bg-word :refer :all]
            [bogo-clojure.bg-char :refer :all]))

(defn has-char?
  [word set]
  (let [nword (normalize word)]
    (reduce #(or %1 (not= -1 (.indexOf nword %2)))
            false
            set))
  )

(defn rollback
  [word transform]
  (let [[first-consonant vowel last-consonant] (split-word word)]
    (cond
      (= :bar transform)
      (string/join [(add-mark-word first-consonant :nomark)
                    vowel
                    last-consonant])
      (accent? transform)
      (remove-accent-word word)
      (mark? transform)
      (string/join [first-consonant
                    (add-mark-word vowel :nomark)
                    last-consonant])
      :else word)))

(defn process-mark
  [word mark key]
  (let [new-word (add-mark-word word mark)]
    (if (not= new-word word)
      new-word
      (str (rollback new-word mark) key))))

(defn process-accent
  [word accent key]
  (let [new-word (add-accent-word word accent)]
    (if (not= new-word word)
      new-word
      (str (rollback new-word accent)
           key))))

(defmacro add-mark
  [key & pairs]
  `(fn [~'word]
     ~(concat '(cond)
              (loop [mark-set pairs
                     actions  '()]
                (if (nil? mark-set)
                  actions
                  (recur (next (next mark-set))
                         (concat actions
                                 `((has-char? ~'word ~(second mark-set))
                                   (process-mark ~'word ~(first mark-set) ~key))))))
              `(:else (str ~'word ~key)))))

(defmacro add-accent
  [key accent]
  `(fn [word#]
     (process-accent word# ~accent ~key)))