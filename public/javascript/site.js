$(function(){

    /**
     * Declare Modules
     **/
    function frontPageModule() {
        var $searchSelect = $("#cityCode");
        var cityCode = $("#city-code").val();
        var actualChart = null;

        // configure front page select
        $searchSelect.select2({
          placeholder: "Ex.: Bauru, Campinas, São Manuel, etc.",
          language: "pt-BR",
          allowClear: true,
          minimumInputLength: 3,
          theme: "bootstrap",
          ajax : {
                    url: "/search/cities",
                    dataType: 'json',
                    contentType: 'application/json',
                    type: "POST",
                    data: function (params) {
                              return JSON.stringify({
                                cityName: params.term, // search term
                             });
                    },
                    results: function (data, page) {
                                return {
                                    results: data.results
                                };
                    },
                    cache: false
          },
          escapeMarkup: function (markup) { return markup; }, // let our custom formatter work
          minimumInputLength: 1,
          templateResult: function (option) {
                              return "<div>" + option.name +" - "+ option.state +"</div>";
                          },
          templateSelection: function (option) {
                               return option.name;
                             } // omitted for brevity, see the source of this page
        });

        function onLoad() {
            // Set Callback to Tab-Anchors
            $('a[data-toggle="tab"]').on('shown.bs.tab', function (e) {
              var year = $(this).attr("data-year");
              loadCharts(year, cityCode);
//              return true;
            });
//            $("#ul-years").delegate("a", "click", function (element) {
//                var year = $(this).attr("data-year");
//               loadCharts(year, cityCode);
//                return true;
//            });

            // Add First Panel to the first tab, if it exists
            var $yearTabs = $(".year-tab");
            var hasChilds = $yearTabs.children().length > 0;
            if (hasChilds) {
                // get first active
                var $firstTab = $($yearTabs[0]);
                var $firstAnchor = $($yearTabs.children()[0]);
                var year = $firstAnchor.attr("data-year");

                var $firstPane = $("#tab-pane-"+year);
                $firstTab.toggleClass("active");

                $firstPane.toggleClass("in active");
                loadCharts(year, cityCode);
            }
        }

        function loadCharts(year, cityCode){
            var chartId = "#chart-"+year;
            var forPostYear = year.length > 4 ? year.replace("-", "") : year;
            var chartCanvas = $(chartId).get(0).getContext("2d");
            $.ajax({ url: "/search/profiles/agegroup",
                     dataType: 'json',
                     cache: false,
                     contentType: "application/json",
                     type: "POST",
                     data: JSON.stringify({
                         year: forPostYear,
                         code: cityCode
                     }),
                     success: function (data) {
                         actualChart = reloadChartCanvas(chartCanvas, data.profiles);
                     },
                });
        }

        function reloadChartCanvas(chartCanvas, profiles) {
            var groupsLabels = profiles.map(function(e) {
                return e.ageGroup;
            });
            console.log(chartCanvas);
            var chartData = {
                 labels : groupsLabels,
                 responsive : true,
                 datasets : [
                     {
                         fillColor : "rgba(172,194,132,0.4)",
                         strokeColor : "#ACC26D",
                         pointColor : "#fff",
                         pointStrokeColor : "#9DB86D",
                         data : profiles.map(function(e) {
                             return e.profilesBySex[0].peoples;
                         })
                     }
                 ]
            };
            console.log(chartCanvas.width);
            console.log(chartCanvas.height);
            $(chartCanvas).css({
                "width": 500,
                "height": 300
            });
            return new Chart(chartCanvas).Bar(chartData);
        }

        onLoad();
    }
    // Load modules
    frontPageModule();
});