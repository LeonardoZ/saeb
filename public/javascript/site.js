$(function(){

    /**
     * Declare Modules
     **/
    function frontPageModule() {
        var $searchSelect = $("#cityCode");
        var cityCode = $("#city-code").val();
        var yearAlreadyLoaded = [];

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
            var $yearTabs = $(".year-tab");
            var hasChilds = $yearTabs.children().length > 0;

            // Set Callback to Tab-Anchors
            $('a[data-toggle="tab"]').on('shown.bs.tab', function (e) {
              var year = $(this).attr("data-year");
              if (!isAlreadyLoaded(year)){
                 loadCharts(year, cityCode);
                 yearAlreadyLoaded.push(year);
              }
            });

            // unnecessary reload prevention
            function isAlreadyLoaded(year){
                var yearPointer;
                for (var i = 0; i < yearAlreadyLoaded.length; i++) {
                    if (yearAlreadyLoaded[i] === year) {
                        return true;
                    }
                }
                return false;
            }

            // true: set the first pane to active state and load the charts
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

        function loadCharts(year, cityCode) {
            var yearInt = Number.parseInt(year.replace("-", ""));
            var forPostYear = year.length > 4 ? year.replace("-", "") : year;

            var chartAgeGroupId = "#chart-"+year;
            var chartSchoolingId = "#chart-sch-"+year;

            var chartAgeGroupCanvas = $(chartAgeGroupId).get(0).getContext("2d");
            var chartSchoolingCanvas = $(chartSchoolingId).get(0).getContext("2d");
            baseAjaxRequest("/search/profiles/agegroup",
                { year: forPostYear, code: cityCode },
                function (data) {
                    reloadChartCanvas(chartAgeGroupCanvas, data.profiles);
            });
            baseAjaxRequest("/search/profiles/schooling",
                { year: forPostYear, code: cityCode },
               function (data) {
                   if (data.profiles.length > 1){
                       reloadSchoolingChartCanvas(chartSchoolingCanvas, data.profiles);
                   } else {
                       var $divSchooling = $("#row-sch-"+year);
                       $divSchooling.remove();
                   }
            });
        }

        function baseAjaxRequest(endpoint, postData, successCallback) {
            $.ajax({ url: endpoint,
                dataType: 'json',
                cache: false,
                contentType: "application/json",
                type: "POST",
                data: JSON.stringify(postData),
                success: successCallback
            });
        }

        function reloadChartCanvas(chartCanvas, profiles) {
            var groupsLabels = profiles.map(function(e) {
                return e.ageGroup;
            });
            var chartData = {
                labels : groupsLabels,
                responsive : true,
                scales: {
                    yAxes: [{
                      scaleLabel: {
                        display: true,
                        labelString: 'probability'
                      }
                    }]
                 },
                 options: {
                    scales: {
                                yAxes: [{
                                    ticks: {
                                        beginAtZero:true
                                    }
                                }]
                    },
                    title: {
                        display: true,
                        text: "Quantidade de eleitores distribuídos por faixa etária"
                    }
                 }
            };
            var dataM = profiles.map(function(e) {
                return e.profilesBySex[0].peoples;
            });

            var dataF = profiles.map(function(e) {
                return e.profilesBySex[1].peoples;
            });

            var dataN = profiles.map(function(e) {
                console.log(e.profilesBySex.toString());
                return (e.profilesBySex.length > 2) ? e.profilesBySex[2].peoples : 0;
            });

            chartData.datasets = [
                     {
                         label: "Mulher",
                         xAxisID: "Faixa etária",
                         yAxisID: "Quantidade de eleitores",
                         fillColor : "#e39292",
                         strokeColor : "#e57575",
                         pointColor : "#fff",
                         pointStrokeColor : "#b86da6",
                         data : dataF
                     },{
                         label: "Homem",
                         fillColor : "#849bc2",
                         strokeColor : "#6d8cc2",
                         pointColor : "#fff",
                         pointStrokeColor : "#6db2b8",
                         data : dataM
                     },
                     {
                         label: "Não informado",
                         fillColor : "rgba(172,194,132,0.4)",
                         strokeColor : "#ACC26D",
                         pointColor : "#fff",
                         pointStrokeColor : "#9DB86D",
                         data : dataN
                     }
                ];
            $(chartCanvas).css({
                "width": 750,
                "height": 500
            });
            return new Chart(chartCanvas).Bar(chartData);
        }


        function reloadSchoolingChartCanvas(chartCanvas, profiles) {

                    var schoolingLabels = profiles.map(function(e) {
                            return e.schooling;
                    });

                    var chartData = {
                        labels : schoolingLabels,
                        responsive : true,
                        scales: {
                            yAxes: [{
                              scaleLabel: {
                                display: true,
                                labelString: 'probability'
                              }
                            }]
                         },
                         options: {
                            scales: {
                                        yAxes: [{
                                            ticks: {
                                                beginAtZero:true
                                            }
                                        }]
                            },
                            title: {
                                display: true,
                                text: "Quantidade de eleitores distribuídos por escolaridade"
                            }
                         }
                    };
                    var dataM = profiles.map(function(e) {
                        return e.profilesBySex[0].peoples;
                    });

                    var dataF = profiles.map(function(e) {
                        return e.profilesBySex[1].peoples;
                    });

                    var dataN = profiles.map(function(e) {
                        console.log(e.profilesBySex.toString());
                        return (e.profilesBySex.length > 2) ? e.profilesBySex[2].peoples : 0;
                    });

                    chartData.datasets = [
                             {
                                 label: "Mulher",
                                 yAxisID: "Quantidade de eleitores",
                                 fillColor : "#e39292",
                                 strokeColor : "#e57575",
                                 pointColor : "#fff",
                                 pointStrokeColor : "#b86da6",
                                 data : dataF
                             },{
                                 label: "Homem",
                                 fillColor : "#849bc2",
                                 strokeColor : "#6d8cc2",
                                 pointColor : "#fff",
                                 pointStrokeColor : "#6db2b8",
                                 data : dataM
                             },
                             {
                                 label: "Não informado",
                                 fillColor : "rgba(172,194,132,0.4)",
                                 strokeColor : "#ACC26D",
                                 pointColor : "#fff",
                                 pointStrokeColor : "#9DB86D",
                                 data : dataN
                             }
                    ];

                    $(chartCanvas).css({
                        "width": 750,
                        "height": 500
                    });
                    return new Chart(chartCanvas).Bar(chartData);
        }

        onLoad();
    }
    // Load modules
    frontPageModule();
});