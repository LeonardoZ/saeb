$(function(){

    /**
     * Declare Modules
     **/
    function frontPageModule() {
        var $searchSelect = $("#cityCode");

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
    }

    function cityPageModule(){
            var cityCode = $("#city-code").val();
            var yearAlreadyLoaded = [];

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
                    yearAlreadyLoaded.push(year);
                }

                // load simpler charts of city page
                // those chart doens't need to know the selected year
                loadSimplerCharts(cityCode);
            }

            function loadSimplerCharts(cityCode) {
                baseAjaxRequest("/profiles/year",
                  { cityCode: cityCode },
                  function (data) {
                     // load chart
                      var $canvas = $("#chart-peoples-by-year").get(0).getContext("2d");
                      loadPeopleByYearChart($canvas, data.profiles)
                  }
                );
            }

            function loadPeopleByYearChart($canvas, profiles) {

                var labels =  profiles.map(function(e) {
                    return e.yearMonth;
                });

               var dataM = profiles.map(function(e) {
                    return e.peoplesBySex[0].peoples;
                });

                var dataF = profiles.map(function(e) {
                    return e.peoplesBySex[1].peoples;
                });

                var dataN = profiles.map(function(e) {
                    return (e.peoplesBySex.length > 2) ? e.peoplesBySex[2].peoples : 0;
                });

                var chartData = {
                    type: "line",
                    options: {
                        tooltips: {
                            callbacks: {
                                label: function(tooltipItems, data) {
                                    console.log(tooltipItems);
                                    console.log(data);
                                    var pre = data.datasets[tooltipItems.datasetIndex].label;
                                    return pre + ": " + numeral(tooltipItems.yLabel).format('0,0');
                                }
                            }
                        },
                        scales: {
                            yAxes: [{
                                ticks: {
                                    beginAtZero: false,
                                    callback: function(value, index, values) {
                                        return numeral(value).format('0,0');
                                    }
                                }
                            }]
                        },
                        title: {
                            display: true,
                            text: "Evolução do número de eleitores ao longo dos últimos anos"
                        }
                    },
                    data: {
                        labels: labels,
                        datasets: [
                            {
                                pointRadius: 10,
                                spanGaps: true,
                                fill: false,
                                label: "Feminino",
                                backgroundColor : "#e39292",
                                strokeColor : "#e57575",
                                data : dataF
                            },{
                                pointRadius: 10,
                                fill: false,
                                label: "Masculino",
                                backgroundColor : "#849bc2",
                                strokeColor : "#6d8cc2",
                                data : dataM
                            }

                        ]
                    }
                };

               $($canvas).css({
                   "width": 750,
                   "height": 400
               });
               var chart = new Chart($canvas, chartData);
            }

            function parseYear(year) {
                var yearInt = Number.parseInt(year.replace("-", ""));
                var forPostYear = year.length > 4 ? year.replace("-", "") : year;
                return forPostYear;
            }

            function loadCharts(year, cityCode) {
                var forPostYear = parseYear(year);

                var chartAgeGroupId = "#chart-"+year;
                var chartSchoolingId = "#chart-sch-"+year;
                var chartSexId = "#chart-combined-"+year;

                var $chartAgeGroupCanvas = $(chartAgeGroupId).get(0).getContext("2d");
                var $chartSchoolingCanvas = $(chartSchoolingId).get(0).getContext("2d");
                var $chartSexCanvas = $(chartSexId).get(0).getContext("2d");

                var $legendsSex = $("#legend-combined-" + year);
                var $legendsSchooling = $("#legend-sch-" + year);
                var $legendsAgeGroup = $("#legend-age-" + year);

                baseAjaxRequest("/profiles/agegroup",
                    { year: forPostYear, code: cityCode },
                    function (data) {
                        // make age group analyzes available
                        createAvailableAnalyzesItem(year, "age", "Faixa etária", "users");
                        // load chart
                        reloadChartCanvas($chartAgeGroupCanvas, $legendsAgeGroup,data.profiles);
                    });

                baseAjaxRequest("/profiles/schooling",
                   { year: forPostYear, code: cityCode },
                   function (data) {
                       if (data.profiles.length > 1) {
                           // make analyzes available
                           createAvailableAnalyzesItem(year, "sch", "Escolaridade", "graduation-cap");

                           // load chart
                           reloadSchoolingChartCanvas($chartSchoolingCanvas, $legendsSchooling, data.profiles);
                       } else {
                           var $divSchooling = $("#row-sch-"+year);
                           $divSchooling.remove();
                       }
                });

                baseAjaxRequest("/profiles/sex",
                       { year: forPostYear, code: cityCode },
                       function (data) {
                           if (data.profiles.length > 1) {
                               // make analyzes available
                               createAvailableAnalyzesItem(year, "combined", "Sexo", "venus-mars");
                               // load chart
                               reloadChartSexCanvas($chartSexCanvas, $legendsSex, data.profiles);
                           } else {
                               var $divSchooling = $("#row-combined-"+year);
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

            function createAvailableAnalyzesItem(year, typeId, displayName, fontAwesomeIconName){

                var $li = $("<li />", {
                    class : "list-group-item"
                });

                var $i = $("<i />", {
                    class : "fa fa-" + fontAwesomeIconName,
                    html : "&nbsp"
                });

                var $a = $("<a />", {
                    id : "#row-" + typeId + "-" + year,
                    href : "#row-" + typeId + "-" + year,
                    html : "<strong>" + displayName + "</strong>"
                });

                $li.append($i);
                $li.append($a);
                $("#analyzes-" + year).append($li)
            }

            function createChartData($chartCanvas, $legend, profiles, title, labels) {
                var dataM = profiles.map(function(e) {
                    return e.profilesBySex[0].peoples;
                });

                var dataF = profiles.map(function(e) {
                    return e.profilesBySex[1].peoples;
                });

                var dataN = profiles.map(function(e) {
                    return (e.profilesBySex.length > 2) ? e.profilesBySex[2].peoples : 0;
                });

                var chartData = {
                    type: "bar",
                    options: {
                        tooltips: {
                            callbacks: {
                                label: function(tooltipItems, data) {
                                    console.log(tooltipItems);
                                    console.log(data);
                                    var pre = data.datasets[tooltipItems.datasetIndex].label;
                                    return pre + ": " + numeral(tooltipItems.yLabel).format('0,0');
                                }
                            }
                        },
                        scales: {
                            yAxes: [{
                                ticks: {
                                    beginAtZero: true,
                                    callback: function(value, index, values) {
                                        return numeral(value).format('0,0');
                                    }
                                }
                            }]
                        },
                        title: {
                            display: true,
                            text: title
                        }
                    },
                    data: {
                        labels: labels,
                        datasets: [
                            {
                                label: "Feminino",
                                backgroundColor : "#e39292",
                                strokeColor : "#e57575",
                                pointColor : "#fff",
                                pointStrokeColor : "#b86da6",
                                data : dataF
                            },{
                                label: "Masculino",
                                backgroundColor : "#849bc2",
                                strokeColor : "#6d8cc2",
                                pointColor : "#fff",
                                pointStrokeColor : "#6db2b8",
                                data : dataM
                            },
                            {
                                label: "Não informado",
                                backgroundColor : "rgba(172,194,132,0.4)",
                                strokeColor : "#ACC26D",
                                pointColor : "#fff",
                                pointStrokeColor : "#9DB86D",
                                data : dataN
                            }
                        ]
                    }
                };

                $($chartCanvas).css({
                    "width": 750,
                    "height": 500
                });
                return chartData;
            }

            function reloadChartCanvas($chartCanvas, $legend , profiles) {
                var groupsLabels = profiles.map(function(e) {
                    return e.ageGroup;
                });

                var title = "Quantidade de eleitores distribuídos por faixa etária";
                var chartData = createChartData($chartCanvas, $legend, profiles, title, groupsLabels);
                var chart = new Chart($chartCanvas, chartData);
                return chart;
            }


            function reloadSchoolingChartCanvas($chartCanvas, $legend, profiles) {

                var schoolingLabels = profiles.map(function(e) {
                    return e.schooling;
                });

                var title = "Quantidade de eleitores distribuídos por nível de escolaridade";
                var chartData = createChartData($chartCanvas, $legend, profiles, title, schoolingLabels);

                var chart = new Chart($chartCanvas, chartData);
                return chart;
            }

            function reloadChartSexCanvas($chartCanvas, $legend, profiles) {
                    var groupsLabels = profiles.map(function(e) {
                        return e.sex;
                    });

                    var dataM = profiles[0].peoples;

                    var dataF = profiles[1].peoples;

                    var dataN = (profiles.length > 2) ? profiles[2].peoples : 0;

                    var chartData = {
                        type: "doughnut",
                        options: {
                            title: {
                                display: true,
                                text: "Proporção entre eleitores dos sexos registrados"
                            }
                        },
                        data: {
                            labels: ["Feminino", "Masculino", "Não Informado"],
                            datasets: [
                                {
                                    data: [dataF, dataM, dataN],
                                    backgroundColor: [
                                        "#e39292",
                                        "#849bc2",
                                        "#rgba(172,194,132,0.4)"
                                    ]
                                }
                            ]
                        }
                    };

                    $($chartCanvas).css({
                        "width": 750,
                        "height": 500
                    });


                    var chart = new Chart($chartCanvas, chartData);
                    return chart;
                }


            onLoad();
    }

    // Load modules
    frontPageModule();
    cityPageModule();

});