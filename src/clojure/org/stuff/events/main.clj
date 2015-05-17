; (in-ns 'org.stuff.events.main)

(ns org.stuff.events.main
  (:use
    [neko.ui.mapping :only [defelement]])
  (:require
          [neko.resource :refer :all]
          [neko.activity :refer [defactivity set-content-view! *a]]
          [neko.threading :refer [on-ui]]
          [neko.ui :refer [make-ui config]]
          [neko.notify :refer [toast]]
          [neko.find-view :refer [find-view]]
          [clojure.string :refer [join]]
          [net.nightweb.dialogs :as dialogs]
          [org.stuff.events.api :as api]
          [clojure.data.json :as json]
          [neko.log :as log])
  (:import  android.widget.TextView
            android.view.View
            android.content.Intent
            (android.graphics Color)
            (android.app Activity)))

(import-all)

(defn set-elmt [activity elmt s]
  (on-ui (config (find-view activity elmt) :text s)))

(defn hide-elmt [activity elmt]
  (on-ui (config (find-view activity elmt) :visibility android.view.View/INVISIBLE)))

(defn show-elmt [activity elmt]
  (on-ui (config (find-view activity elmt) :visibility android.view.View/VISIBLE)))

; (defn start-activity [context activity]
;   (.startActivity context (Intent. context (resolve activity))))

(defn hourly-layout [activity]
  [:relative-layout {}
    [ :text-view {
      :text "HOURLY TEXT VIEW"
    }]])

(defactivity org.stuff.events.HourlyActivity
  :key :hourly
  :on-create
    (fn [this bundle]
      (on-ui (set-content-view! (*a) (hourly-layout (*a))))))

(defn daily-layout [activity]
  [:relative-layout {}
    [ :text-view {
      :text "DAILY TEXT VIEW"
    }]])

(defn update-ui [context forecast]
  (let [city (:name forecast)
        humidity (str (:humidity (:main forecast)) "%")
        wind-speed (str (:speed (:wind forecast)) " MPH")
        temperature (str (format "%.0f" (:temp (:main forecast))))
        description (:description (first (:weather forecast)))]
    (set-elmt context ::location-label city)
    (set-elmt context ::description-label description)
    (set-elmt context ::temperature-label temperature)
    (set-elmt context ::humidity-value humidity)
    (set-elmt context ::wind-speed-value wind-speed)
    (hide-elmt context ::search-status-label)
    (show-elmt context ::update-button)
    ))

(defn async-request [context request]
  (future
    (on-ui (.setProgressBarIndeterminateVisibility context true))
    (try
      (show-elmt context ::search-status-label)
      (hide-elmt context ::update-button)
      (let [response (request)
            json (json/read-str response :key-fn keyword)]
        (update-ui context json))
      (catch Exception ex#
        (on-ui
          (hide-elmt context ::search-status-label)
          (show-elmt context ::update-button)
          (toast context (str ex#))))
      (finally (on-ui (.setProgressBarIndeterminateVisibility context false))))))

(defn prompt-for-postal-code-and-update-ui []
  (on-ui (dialogs/show-input-dialog (*a) (fn [postal-code]
    (let [request (partial api/fetch-forecast postal-code)]
      (async-request (*a) request))))))

(defactivity org.stuff.events.DailyActivity
  :key :daily
  :on-create
    (fn [this bundle]
      (on-ui (set-content-view! (*a) (daily-layout (*a))))))

(defelement  :image-view
  :classname android.widget.ImageView
  :inherits  :view
  :attributes {:image-resource org.stuff.events.R$drawable/cloudy_night})

(defn main-layout [activity]
  [:relative-layout {
                      :background-resource org.stuff.events.R$drawable/bg_gradient
                      :padding-top [16 :dp]
                      :padding-right [32 :dp]
                      :padding-bottom [16 :dp]
                      :padding-left [32 :dp]
                    }

                    [:text-view {
                                  :id ::temperature-label
                                  :text "99"
                                  :layout-width :wrap
                                  :layout-height :wrap
                                  :layout-center-vertical true
                                  :layout-center-horizontal true
                                  :text-color (android.graphics.Color/WHITE)
                                  :text-size [150 :sp]
                                }]

                    [:image-view {
                                   :id ::degree-image-view
                                   :image-resource org.stuff.events.R$drawable/degree
                                   :layout-align-top ::temperature-label
                                   :layout-height :wrap
                                   :layout-margin-top [50 :dp]
                                   :layout-to-end-of ::temperature-label
                                   :layout-to-right-of ::temperature-label
                                   :layout-width :wrap
                                 }]

                    [:text-view {
                                  :id ::description-label
                                  :layout-above ::degree-image-view
                                  :layout-center-horizontal true
                                  :layout-height :wrap
                                  :layout-width :wrap
                                  :text "Clear Skies"
                                  :text-color (android.graphics.Color/WHITE)
                                  :text-size [18 :sp]
                                }]

                    [:text-view {
                                  :id ::location-label
                                  :layout-above ::description-label
                                  :layout-center-horizontal true
                                  :layout-height :wrap
                                  :layout-margin-bottom [60 :dp]
                                  :layout-width :wrap
                                  :text "Alcatraz Island, CA"
                                  :text-color (android.graphics.Color/WHITE)
                                  :text-size [24 :sp]
                                }]

                    ; [:image-view {
                    ;                :id ::icon-image-view
                    ;                :image-resource org.stuff.events.R$drawable/cloudy_night
                    ;                :layout-align-bottom ::location-label
                    ;                :layout-width :wrap
                    ;                :layout-height :wrap
                    ;                :layout-align-parent-left true
                    ;                :layout-align-parent-start true
                    ;              }]

                    [:linear-layout {
                                      :id ::linear-layout
                                      :layout-below ::temperature-label
                                      :layout-center-horizontal true
                                      :layout-height :wrap
                                      :layout-margin-top [10 :dp]
                                      :layout-width :fill
                                      :orientation :horizontal
                                      ; :weight-sum 100
                                    }

                                    [:linear-layout {
                                                      :layout-height :fill
                                                      :layout-weight 50
                                                      :layout-width :wrap
                                                      :orientation :vertical
                                                    }

                                                    [:text-view {
                                                                  :gravity android.view.Gravity/CENTER_HORIZONTAL
                                                                  :id ::humidity-label
                                                                  :layout-height :wrap
                                                                  :layout-width :fill
                                                                  :text-color (android.graphics.Color/WHITE)
                                                                  :text (get-string org.stuff.events.R$string/humidity_label)
                                                                  :text-size [18 :sp]
                                                                }]

                                                    [:text-view {
                                                                  :gravity android.view.Gravity/CENTER_HORIZONTAL
                                                                  :id ::humidity-value
                                                                  :layout-height :wrap
                                                                  :layout-width :fill
                                                                  :text "100%"
                                                                  :text-color (android.graphics.Color/WHITE)
                                                                  :text-size [24 :sp]
                                                                }]
                                                    ]

                                    [:linear-layout {
                                                      :layout-height :fill
                                                      :layout-weight 50
                                                      :layout-width :wrap
                                                      :orientation :vertical
                                                    }

                                                    [:text-view {
                                                                  :gravity android.view.Gravity/CENTER_HORIZONTAL
                                                                  :id ::wind-speed-label
                                                                  :layout-height :wrap
                                                                  :layout-width :fill
                                                                  :text-color (android.graphics.Color/WHITE)
                                                                  :text (get-string org.stuff.events.R$string/wind_speed_label)
                                                                  :text-size [18 :sp]
                                                                }]

                                                    [:text-view {
                                                                  :gravity android.view.Gravity/CENTER_HORIZONTAL
                                                                  :id ::wind-speed-value
                                                                  :layout-height :wrap
                                                                  :layout-width :fill
                                                                  :text "10 MPH"
                                                                  :text-color (android.graphics.Color/WHITE)
                                                                  :text-size [24 :sp]
                                                                }]

                                                    ]

                                    ]

                    [:text-view {
                                  :gravity android.view.Gravity/CENTER_HORIZONTAL
                                  :id ::search-status-label
                                  :layout-below ::linear-layout
                                  :layout-center-horizontal true
                                  :layout-height :wrap
                                  :layout-margin-top [40 :dp]
                                  :layout-width :wrap
                                  :text-color (android.graphics.Color/WHITE)
                                  :text (get-string org.stuff.events.R$string/search_status_label)
                                  :text-size [18 :sp]
                                  :visibility android.view.View/INVISIBLE
                                }]

                    [:image-view {
                                   :id ::refresh-image-view
                                   :image-resource org.stuff.events.R$drawable/refresh
                                   :layout-align-parent-top true
                                   :layout-center-horizontal true
                                   :layout-height :wrap
                                   :layout-width :wrap
                                 }]

                    ; [:progress-bar {
                    ;                  :id ::progress-bar
                    ;                  :layout-align-bottom ::refresh-image-view
                    ;                  :layout-align-parent-top true
                    ;                  :layout-center-horizontal true
                    ;                  :layout-height :wrap
                    ;                  :layout-width :wrap
                    ;                }]

                     [:linear-layout {
                                       :layout-align-parent-bottom true
                                       :layout-align-parent-left true
                                       :layout-align-parent-start true
                                       :layout-height :wrap
                                       :layout-width :fill
                                       :orientation :horizontal
                                     }

                                     [:button {
                                                :background-color (android.graphics.Color/WHITE)
                                                :id ::update-button
                                                :layout-height :fill
                                                :layout-weight 1
                                                :layout-width :fill
                                                :text-color (android.graphics.Color/BLACK)
                                                :text "Update Location"
                                                :on-click (fn [_] (prompt-for-postal-code-and-update-ui))
                                                :visibility android.view.View/INVISIBLE
                                              }]
                                     ]

                    ])

(defactivity org.stuff.events.MainActivity
  :key :main
  :on-create
  (fn [this bundle]
    (on-ui (set-content-view! (*a) (main-layout (*a))))
    (prompt-for-postal-code-and-update-ui)))
