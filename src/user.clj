(ns user
  (:import [java.util UUID])
  (:require [clojure.edn :as edn :refer [read-string]]
            #_[util :refer [string->uuid string->filesystem-path-ready]]
            [clojure.pprint :refer [pprint]]
            [clojure.java.io :as io]))
