(ns util)

(defn rand-int-from-to
  "returns a random integer from to"
  [from to]
  (let [min-value from
        max-value to]
    (+ min-value (rand-int  (- max-value min-value)))))
