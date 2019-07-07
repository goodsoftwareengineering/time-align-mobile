(ns time-align-mobile.screens.pattern-planning
  (:require [time-align-mobile.js-imports :refer [view
                                                  text
                                                  mi
                                                  mci
                                                  status-bar
                                                  touchable-highlight]]
            ["react-native-elements" :as rne]
            [time-align-mobile.styles :as styles]
            [oops.core :refer [oget oset! ocall oapply ocall! oapply!
                               oget+ oset!+ ocall+ oapply+ ocall!+ oapply!+]]
            [time-align-mobile.helpers :as helpers]
            [re-frame.core :refer [subscribe dispatch]]
            [com.rpl.specter :as sp :refer-macros [select select-one setval transform]]
            [time-align-mobile.components.day :refer [time-indicators
                                                      top-bar-outer-style
                                                      bottom-bar
                                                      render-period
                                                      selection-menu
                                                      get-touch-info-from-event
                                                      selection-menu-button-row-style
                                                      selection-menu-button
                                                      selection-menu-button-container-style
                                                      selection-menu-button-container-style
                                                      padding]]
            [reagent.core :as r]))

(defn templates-comp [{:keys [templates dimensions selected-template]}]
  [view
   (doall
    (->> templates
         (map (fn [collision-group]
                (doall
                 (->> collision-group
                      (map-indexed
                       (fn [index {:keys [start stop] :as template}]
                         (let [now (js/Date.)]
                           (render-period
                            {:entity (merge template  ;; TODO refactor :period key?
                                            {:start   (helpers/reset-relative-ms
                                                       start now)
                                             :stop    (helpers/reset-relative-ms
                                                       stop now)
                                             :planned true})

                             :transform-functions       {:up            (fn [] #(println "transform function"))
                                                         :down          (fn [] #(println "transform function"))
                                                         :start-earlier (fn [] #(println "transform function"))
                                                         :stop-earlier  (fn [] #(println "transform function"))
                                                         :stop-later    (fn [] #(println "transform function"))
                                                         :start-later   (fn [] #(println "transform function"))}
                             :entity-type               :template
                             :collision-index           index
                             :collision-group-size      (count collision-group)
                             :displayed-day             now
                             :dimensions                dimensions
                             :selected-entity           selected-template
                             :select-function-generator (fn [id]
                                                          #(dispatch [:select-template id]))
                             :period-in-play            nil}))))))))))])

(defn selection-menu-buttons [selected-template pattern-form]
  (let [row-style {:style selection-menu-button-row-style}]
    [view {:style selection-menu-button-container-style}
     ;; edit
     [view row-style
      [selection-menu-button
       "edit"
       [mi {:name "edit"}]
       (fn [_]
         (dispatch [:navigate-to
                    {:current-screen :template
                     :params         {:template-id             (:id selected-template)
                                      :pattern-form-pattern-id (:id pattern-form)}}]))]]

     ;; start-later
     [view row-style
      [selection-menu-button
       "start later"
       [mci {:name "arrow-collapse-down"}]
       (fn [_]
         ;; TODO stop from moving past stop ? or does spec do that?
         (dispatch [:update-pattern-form
                    (select-keys (transform
                                  [:templates sp/ALL
                                   #(= (:id %) (:id selected-template))
                                   :start]
                                  #(+ (helpers/minutes->ms 5) %)
                                  pattern-form)
                                 [:templates])]))
       (fn [_]
         ;; TODO stop from moving past stop
         (dispatch [:update-pattern-form
                    (select-keys (transform
                                  [:templates sp/ALL
                                   #(= (:id %) (:id selected-template))
                                   :start]
                                  #(+ (helpers/hours->ms 3) %)
                                  pattern-form)
                                 [:templates])]))]]

     ;; start-earlier
     [view row-style
      [selection-menu-button
       "start earlier"
       [mci {:name "arrow-expand-up"}]
       (fn [_]
         ;; TODO stop from moving below 0
         (dispatch [:update-pattern-form
                    (select-keys (transform
                                  [:templates sp/ALL
                                   #(= (:id %) (:id selected-template))
                                   :start]
                                  #(- % (helpers/minutes->ms 5))
                                  pattern-form)
                                 [:templates])]))
       (fn [_]
         ;; TODO stop from moving below 0
         (dispatch [:update-pattern-form
                    (select-keys (transform
                                  [:templates sp/ALL
                                   #(= (:id %) (:id selected-template))
                                   :start]
                                  #(- % (helpers/hours->ms 3))
                                  pattern-form)
                                 [:templates])]))]]

     ;; up
     [view row-style
      [selection-menu-button
       "up"
       [mi {:name "arrow-upward"}]
       (fn [_]
         (dispatch [:update-pattern-form
                    (select-keys (transform
                                  [:templates sp/ALL
                                   #(= (:id %) (:id selected-template))]
                                  (fn [template]
                                    (->> template
                                         (transform
                                          [:start]
                                          #(max 0 (- % (helpers/minutes->ms 5))))
                                         (transform
                                          [:stop]
                                          #(max 1 (- % (helpers/minutes->ms 5))))))
                                  pattern-form)
                                 [:templates])]))
       (fn [_]
         (dispatch [:update-pattern-form
                    (select-keys (transform
                                  [:templates sp/ALL
                                   #(= (:id %) (:id selected-template))]
                                  (fn [template]
                                    (->> template
                                         (transform
                                          [:start]
                                          #(max 0 (- % (helpers/hours->ms 3))))
                                         (transform
                                          [:stop]
                                          #(max 1 (- % (helpers/hours->ms 3))))))
                                  pattern-form)
                                 [:templates])]))]]

     ;; down
     [view row-style
      [selection-menu-button
       "down"
       [mi {:name "arrow-downward"}]
       (fn [_]
         (dispatch [:update-pattern-form
                    (select-keys (transform
                                  [:templates sp/ALL
                                   #(= (:id %) (:id selected-template))]
                                  (fn [template]
                                    (->> template
                                         (transform
                                          [:start]
                                          #(min (helpers/hours->ms 23.8)
                                                (+ % (helpers/minutes->ms 5))))
                                         (transform
                                          [:stop]
                                          #(min (helpers/hours->ms 23.9)
                                                (+ % (helpers/minutes->ms 5))))))
                                  pattern-form)
                                 [:templates])]))
       (fn [_]
         (dispatch [:update-pattern-form
                    (select-keys (transform
                                  [:templates sp/ALL
                                   #(= (:id %) (:id selected-template))]
                                  (fn [template]
                                    (->> template
                                         (transform
                                          [:start]
                                          #(min (helpers/hours->ms 23.8)
                                                (+ % (helpers/hours->ms 3))))
                                         (transform
                                          [:stop]
                                          #(min (helpers/hours->ms 23.9)
                                                (+ % (helpers/hours->ms 3))))))
                                  pattern-form)
                                 [:templates])]))]]

     ;; stop-later
     [view row-style
      [selection-menu-button
       "stop later"
       [mci {:name "arrow-expand-down"}]
       (fn [_]
         ;; TODO keep from going beyond end of day
         (dispatch [:update-pattern-form
                    (select-keys (transform
                                  [:templates sp/ALL
                                   #(= (:id %) (:id selected-template))
                                   :stop]
                                  #(+ % (helpers/minutes->ms 5))
                                  pattern-form)
                                 [:templates])]))
       (fn [_]
         ;; TODO keep from going beyond end of day
         (dispatch [:update-pattern-form
                    (select-keys (transform
                                  [:templates sp/ALL
                                   #(= (:id %) (:id selected-template))
                                   :stop]
                                  #(+ % (helpers/hours->ms 3))
                                  pattern-form)
                                 [:templates])]))]]

     ;; stop-earlier
     [view row-style
      [selection-menu-button
       "stop earlier"
       [mci {:name "arrow-collapse-up"}]
       (fn [_]
         ;; TODO keep from going before start
         (dispatch [:update-pattern-form
                    (select-keys (transform
                                  [:templates sp/ALL
                                   #(= (:id %) (:id selected-template))
                                   :stop]
                                  #(- % (helpers/minutes->ms 5))
                                  pattern-form)
                                 [:templates])]))
       (fn [_]
         ;; TODO keep from going before start
         (dispatch [:update-pattern-form
                    (select-keys (transform
                                  [:templates sp/ALL
                                   #(= (:id %) (:id selected-template))
                                   :stop]
                                  #(- % (helpers/hours->ms 3))
                                  pattern-form)
                                 [:templates])]))]]

     ;; select-prev
     [view row-style
      [selection-menu-button
       "select prev"
       [mci {:name  "arrow-down-drop-circle"
             :style {:transform [{:rotate "180deg"}]}}]
       #(dispatch [:select-next-or-prev-template-in-form :prev])]]

     ;; select-next
     [view row-style
      [selection-menu-button
       "select next"
       [mci {:name "arrow-down-drop-circle"}]
       #(dispatch [:select-next-or-prev-template-in-form :next])]]]))

(defn root []
  (let [pattern-form      (subscribe [:get-pattern-form])
        buckets           (subscribe [:get-buckets]) ;; this is just for selecting a random bucket for new template long press
        changes           (subscribe [:get-pattern-form-changes])
        selected-template (subscribe [:get-selected-template])
        top-bar-height    styles/top-bar-height
        bottom-bar-height styles/bottom-bar-height
        dimensions        (r/atom {:width nil :height nil})]

    (r/create-class
     {:reagent-render
      (fn [params]
        [view {:style {:flex            1
                       :justify-content "flex-start"
                       :align-items     "center"}

               :on-layout
               (fn [event]
                 (let [layout (-> event
                                  (oget "nativeEvent" "layout")
                                  (js->clj :keywordize-keys true))]
                   (if (nil? (:height dimensions))
                     (reset! dimensions {:width  (:width layout)
                                         :height (-
                                                  (:height layout)
                                                  top-bar-height
                                                  bottom-bar-height)}))))}
         ;; top bar stuff
         [status-bar {:hidden true}]
         [view {:style (top-bar-outer-style top-bar-height dimensions)}
          [text (:label @pattern-form)]]

         ;; view that stretches to fill what is left of the screen
         [touchable-highlight
          ;; add new template on long press
          {:on-long-press (fn [evt]
                            (let [{:keys [native-event
                                          location-y
                                          location-x
                                          relative-ms
                                          start]}
                                  (get-touch-info-from-event {:evt           evt
                                                              :dimensions    @dimensions
                                                              :displayed-day (js/Date.)})]
                              (dispatch [:add-new-template-to-planning-form
                                         {:pattern-id (:id @pattern-form)
                                          :start      start
                                          :bucket-id  (->> @buckets
                                                           first
                                                           :id)
                                          :id         (random-uuid)
                                          :now        (js/Date.)}])))}

          [view {:style {:height           (:height @dimensions)
                         :width            (:width @dimensions)
                         :background-color styles/background-color}}

           [time-indicators @dimensions :left]

           ;; templates
           [templates-comp {:templates         (->> @pattern-form
                                                    :templates
                                                    (sort-by :start)
                                                    (helpers/get-collision-groups))
                            :selected-template @selected-template
                            :dimensions        @dimensions}]

           ;; selection menu
           (when (some? @selected-template)
             [selection-menu
              {:selected-period-or-template (-> @selected-template
                                                :id
                                                ;; get the template from the form
                                                ;; not from the pattern
                                                (#(some (fn [template]
                                                          (if (= (:id template)
                                                                 %)
                                                            template
                                                            false))
                                                        (:templates @pattern-form)))
                                                (#(merge
                                                   %
                                                   {:planned true})))
               :type                        :template
               :dimensions                  @dimensions}
              [selection-menu-buttons @selected-template @pattern-form]])]]

         [bottom-bar {:bottom-bar-height bottom-bar-height}
          [:<>
           ;; back button
           [:> rne/Button
            ;; TODO prompt user that this will lose any unsaved changes
            {:icon            (r/as-element [:> rne/Icon {:name  "arrow-back"
                                                          :type  "material-icons"
                                                          :color "#fff"}])
             :on-press        #(dispatch [:navigate-to {:current-screen :pattern
                                                        :params         {:pattern-id (:id @pattern-form)}}])
             :container-style {:margin-right 4}}]

           ;; save button
           [:> rne/Button
            (merge {:container-style {:margin-left 4}
                    :icon            (r/as-element [:> rne/Icon {:name  "save"
                                                                 :type  "font-awesome"
                                                                 :color "#fff"}])
                    :on-press        #(dispatch [:save-pattern-form (js/Date.)])}
                   (when-not (> (count @changes) 0)
                     {:disabled true}))]]]])})))
