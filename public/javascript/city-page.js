 $(function(){

    function cityPageModule(){
            /**
            * There're 2 kind of chart: General and Specific
            * General: Created using all the datasets, so it doesn't need the "Year" information
            * Specific: Is loaded when a specific year is selected. Each year tab has his own canvas for each kind
            * of chart.
            */
            var cityCode = $("#city-code").val();
            var yearAlreadyLoaded = [];
            var dataTableLanguage = {
                   "sEmptyTable": "Nenhum registro encontrado",
                   "sInfo": "Mostrando de _START_ até _END_ de _TOTAL_ registros",
                   "sInfoEmpty": "Mostrando 0 até 0 de 0 registros",
                   "sInfoFiltered": "(Filtrados de _MAX_ registros)",
                   "sInfoPostFix": "",
                   "sInfoThousands": ".",
                   "sLengthMenu": "_MENU_ resultados por página",
                   "sLoadingRecords": "Carregando...",
                   "sProcessing": "Processando...",
                   "sZeroRecords": "Nenhum registro encontrado",
                   "sSearch": "Pesquisar",
                   "oPaginate": {
                     "sNext": "Próximo",
                     "sPrevious": "Anterior",
                     "sFirst": "Primeiro",
                     "sLast": "Último"
                   },
                   "oAria": {
                     "sSortAscending": ": Ordenar colunas de forma ascendente",
                     "sSortDescending": ": Ordenar colunas de forma descendente"
                   },
                   "decimal": ",",
                   "thousands": "."
              }

            function onLoad() {
                var $yearTabs = $(".year-tab");
                var hasChilds = $yearTabs.children().length > 0;

                // Set Callback to Tab-Anchors
                $('a[data-toggle="tab"]').on('shown.bs.tab', function (e) {
                  var year = $(this).attr("data-year");
                  if (!isAlreadyLoaded(year)){
                     loadYearSpecificCharts(year, cityCode);
                     yearAlreadyLoaded.push(year);
                  }
                });


                // true: set the first pane to active state and load the charts
                if (hasChilds) {
                    // get first active
                    var $firstTab = $($yearTabs[0]);
                    var $firstAnchor = $($yearTabs.children()[0]);
                    var year = $firstAnchor.attr("data-year");

                    var $firstPane = $("#tab-pane-"+year);
                    $firstTab.toggleClass("active");

                    $firstPane.toggleClass("in active");
                    loadYearSpecificCharts(year, cityCode);
                    yearAlreadyLoaded.push(year);
                }

                // load simpler charts of city page
                // those chart doens't need to know the selected year
                loadInitialGeneralCharts(cityCode);

                // load summary data
                loadLastYearDataSummary(cityCode);
            }

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

            function loadLastYearDataSummary(cityCode) {
                 $.ajax({ url: "/profiles/last/year",
                    dataType: "html",
                    cache: false,
                    contentType: "application/json",
                    type: "POST",
                    data: JSON.stringify({ cityCode: cityCode }),
                    success: function (data) {
                      var $divCityData = $("#city-data")
                      $divCityData.empty();
                      $divCityData.append($(data));
                      roundUtil();
                  }
                });
            }

            function roundUtil() {
                var formatInt = "0,0";
                var formatPct = "0,00.00";
                var formatPct4 = "0,00.0000";
                $.each($(".round-int"), function(key, value) {
                    var $value = $(value);
                    var numValue = $value.html();
                    $value.text(numeral(numValue).format(formatInt));
                });
                $.each($(".round-pct"), function(key, value) {
                    var $value = $(value);
                    var numValue = $value.html();
                    $value.text(numeral(numValue).format(formatPct));
                });

                $.each($(".round-pct4"), function(key, value) {
                    var $value = $(value);
                    var numValue = $value.html();
                    $value.text(numeral(numValue).format(formatPct4));
                });
            }

            function loadInitialGeneralCharts(cityCode) {
                baseAjaxRequest("/profiles/year/sex",
                  { cityCode: cityCode },
                  function (data) {
                     // load chart
                      var $canvas = $("#chart-peoples-by-year-sex").get(0).getContext("2d");
                      loadPeopleByYearSexGeneralChart($canvas, data.profiles)
                  }
                );

                baseAjaxRequest("/profiles/year",
                  { cityCode: cityCode },
                  function (data) {
                     // load chart
                      var $canvas = $("#chart-peoples-by-year").get(0).getContext("2d");

                      var labels =  data.profiles.peoplesByYear.map(function(e) {
                            return e.yearMonth.length > 4 ? e.yearMonth.substring(0, 4)
                                + " - " + e.yearMonth.substring(4) : e.yearMonth;
                      });

                      var peoples = data.profiles.peoplesByYear.map(function(e) {
                            return e.peoples;
                      });


                      loadSpecificChartSimple($canvas,
                       "Evolução do número de eleitores ao longo dos últimos anos",
                        "#075e89", "Eleitores", labels, peoples, null, "0,0", true)
                  }
                );

                baseAjaxRequest("/profiles/growth",
                  { cityCode: cityCode },
                  function (data) {
                     // load chart
                      var $canvas = $("#chart-growth-by-year").get(0).getContext("2d");

                      var labels =  data.profiles.map(function(e) {
                            return e.range;
                      });

                      var peoples = data.profiles.map(function(e) {
                            return e.value;
                      });

                      loadSpecificChartSimple($canvas,
                       "Taxa de crescimento de eleitores ao longo dos últimos anos",
                        "#045e00", "Eleitores" ,labels, peoples, null, "0.0%", true)
                  }
                );
            }

            function loadPeopleByYearSexGeneralChart($canvas, profiles) {

                var labels =  profiles.map(function(e) {
                    return e.yearMonth.length > 4 ? e.yearMonth.substring(0, 4)
                               + " - " + e.yearMonth.substring(4) : e.yearMonth;;
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
                            text: "Evolução do número de eleitores por sexo ao longo dos últimos anos"
                        },
                        maintainAspectRatio: false

                    },
                    data: {
                        labels: labels,
                        datasets: [
                            {
                                pointRadius: 10,
                                spanGaps: true,
                                lineTension: 0,
                                fill: false,
                                label: "Feminino",
                                backgroundColor : "#f25959",
                                strokeColor : "#f25959",
                                data : dataF
                            },{
                                pointRadius: 10,
                                spanGaps: true,
                                lineTension: 0,
                                fill: false,
                                label: "Masculino",
                                backgroundColor : "#6d8cc2",
                                strokeColor : "#6d8cc2",
                                data : dataM
                            }

                        ]
                    }
                };

               $($canvas).css({
                   "width": 550,
                   "min-height": 350
               });
               var chart = new Chart($canvas, chartData);
            }

            function parseYear(year) {
                var yearInt = Number.parseInt(year.replace("-", ""));
                var forPostYear = year.length > 4 ? year.replace("-", "") : year;
                return forPostYear;
            }

            function loadYearSpecificCharts(year, cityCode) {
                var forPostYear = parseYear(year);

                var chartAgeGroupId = "#chart-"+year;
                var chartAgeGroupUniId = "#chart-uni-"+year;
                var chartSchoolingId = "#chart-sch-"+year;
                var chartSchoolingUniId = "#chart-sch-uni-"+year;
                var chartSexId = "#chart-combined-"+year;

                var $chartAgeGroupCanvas = $(chartAgeGroupId).get(0).getContext("2d");
                var $chartAgeGroupUniCanvas = $(chartAgeGroupUniId).get(0).getContext("2d");
                var $chartSchoolingCanvas = $(chartSchoolingId).get(0).getContext("2d");
                var $chartSchoolingUniCanvas = $(chartSchoolingUniId).get(0).getContext("2d");
                var $chartSexCanvas = $(chartSexId).get(0).getContext("2d");

                var $legendsSex = $("#legend-combined-" + year);
                var $legendsSchooling = $("#legend-sch-" + year);
                var $legendsSchoolingUni = $("#legend-sch-uni-" + year);
                var $legendsAgeUniGroup = $("#legend-age-uni-" + year);
                var $legendsAgeGroup = $("#legend-age-" + year);

                baseAjaxRequest("/profiles/ageandschooling",
                    { year: forPostYear, code: cityCode },
                    function (data) {
                        // make age group analyzes available
                        createAvailableAnalyzesItem(year, "agesch", "Escolaridade e Faixa etária",
                            "sort-numeric-asc");
                        // load chart
                        loadAgeSchoolingSpecificTable(year, data.profiles);
                    });

                baseAjaxRequest("/profiles/agegroup",
                    { year: forPostYear, code: cityCode },
                    function (data) {
                        // make age group analyzes available
                        createAvailableAnalyzesItem(year, "age", "Faixa Etária e Sexo", "users");
                        // load chart
                        loadAgeGroupSpecificChart($chartAgeGroupCanvas, $legendsAgeGroup, data.profiles);
                    });

                baseAjaxRequest("/profiles/agegroup/unified",
                   { year: forPostYear, code: cityCode },
                   function (data) {
                       if (data.profiles.length > 1) {
                           // make analyzes available
                           createAvailableAnalyzesItem(year, "age-uni", "Faixa Etária", "users");
                           var labels =  data.profiles.map(function(e) {
                                return e.ageGroup;
                           });

                           var peoples = data.profiles.map(function(e) {
                                return e.peoples;
                           });

                           var percentOf = data.profiles.map(function(e) {
                                return e.percentOf;
                           });

                          loadSpecificChartSimple($chartAgeGroupUniCanvas,
                           "Eleitores por Faixa Etária",
                            "#ba3232", "Eleitores", labels, peoples, percentOf, null, false)
                       } else {
                           var $divSchooling = $("#row-uni-"+year);
                           $divSchooling.remove();
                       }
                });


                baseAjaxRequest("/profiles/schooling",
                   { year: forPostYear, code: cityCode },
                   function (data) {
                       if (data.profiles.length > 1) {
                           // make analyzes available
                           createAvailableAnalyzesItem(year, "sch", "Escolaridade e Sexo", "graduation-cap");

                           // load chart
                           loadSchoolingSpecificChart($chartSchoolingCanvas, $legendsSchooling, data.profiles);
                       } else {
                           var $divSchooling = $("#row-sch-"+year);
                           $divSchooling.remove();
                       }
                });

                baseAjaxRequest("/profiles/schooling/unified",
                   { year: forPostYear, code: cityCode },
                   function (data) {
                       if (data.profiles.length > 1) {
                           // make analyzes available
                           createAvailableAnalyzesItem(year, "sch-uni", "Escolaridade", "graduation-cap");
                           var labels =  data.profiles.map(function(e) {
                                return e.schooling;
                           });

                           var peoples = data.profiles.map(function(e) {
                                return e.peoples;
                           });

                           var percentOf = data.profiles.map(function(e) {
                                return e.percentOf;
                           });

                          loadSpecificChartSimple($chartSchoolingUniCanvas,
                           "Eleitores por Escolaridade",
                            "#4e4781", "Eleitores", labels, peoples, percentOf, null, false)
                       } else {
                           var $divSchooling = $("#row-sch-uni-"+year);
                           $divSchooling.remove();
                       }
                });

                baseAjaxRequest("/profiles/sex",
                       { year: forPostYear, code: cityCode },
                       function (data) {
                           if (data.profiles.length > 1) {
                               // make analyzes available
                               createAvailableAnalyzesItem(year, "combined", "Proporção dos sexos", "venus-mars");
                               // load chart
                               loadSexSpecificChart($chartSexCanvas, $legendsSex, data.profiles);
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
                    class : "smooth",
                    html : "<strong>" + displayName + "</strong>"
                });

                var $ul = $("#analyzes-" + year);
                $li.append($i);
                $li.append($a);
                $ul.append($li)

                var items = $ul.children("li").get();
                items.sort(function(a, b) {
                   return $(a).text().toUpperCase().localeCompare($(b).text().toUpperCase());
                })
                $.each(items, function(idx, itm) { $ul.append(itm); });
            }

            function loadAgeSchoolingSpecificTable(year, profiles) {
               var format = '0,0';
               var $tbody = $("#schooling-age-group-body-" + year);
               profiles.forEach(function(element) {
                   var tdSchooling = $("<td />", {
                      text: element.schooling
                   });

                   var tdAgeGroup = $("<td />", {
                      text: element.group
                   });

                   var tdPeoples = $("<td />", {
                      class: "text-center",
                      text: numeral(element.peoples).format(format)
                   });

                   var row = $("<tr />");
                   row.append(tdSchooling);
                   row.append(tdAgeGroup);
                   row.append(tdPeoples);

                   $tbody.append(row);
               });
               var $table = $("#schooling-age-group-table-" + year).DataTable({
                    "language": dataTableLanguage,
                    "responsive": true
              });
            }

            function createSpecificDividedBySexChartData($chartCanvas, $legend, profiles, title, labels) {
                var dataM = profiles.map(function(e) {
                    return e.profilesBySex[0].peoples;
                });

                var dataF = profiles.map(function(e) {
                    return e.profilesBySex[1].peoples;
                });

                var dataN = profiles.map(function(e) {
                    return (e.profilesBySex.length > 2) ? e.profilesBySex[2].peoples : 0;
                });

                var dataMP = profiles.map(function(e) {
                    return e.profilesBySex[0].percentOf;
                });

                var dataFP = profiles.map(function(e) {
                    return e.profilesBySex[1].percentOf;
                });

                var dataNP = profiles.map(function(e) {
                    return (e.profilesBySex.length > 2) ? e.profilesBySex[2].percentOf : 0;
                });
                var f  = function(tooltipItems, data) {
                    var pre = data.datasets[tooltipItems.datasetIndex].label;
                    var value = 0.0;
                    if (pre === "Masculino") {
                        value = dataMP[tooltipItems.index];
                    } else if (pre === "Feminino") {
                        value = dataFP[tooltipItems.index];
                    } else {
                        value = dataNP[tooltipItems.index]
                    }
                    return pre + ": " + numeral(tooltipItems.yLabel).format('0,0')+" - "+numeral(value).format("0,00.00") + "%";
                }

                var chartData = {
                    type: "bar",
                    options: {
                        tooltips: {
                            callbacks: {
                                label: f
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
                        },
                        maintainAspectRatio: false

                    },
                    data: {
                        labels: labels,
                        datasets: [
                            {
                                label: "Feminino",
                                backgroundColor : "#ee2929",
                                strokeColor : "#e24d4d",
                                data : dataF
                            },{
                                label: "Masculino",
                                backgroundColor : "#3572dd",
                                strokeColor : "#4b76c1",
                                data : dataM
                            },
                            {
                                label: "Não informado",
                                backgroundColor : "#c34fb5",
                                strokeColor : "#a05697",
                                data : dataN
                            }
                        ]
                    }
                };

                $($chartCanvas).css({
                    "width": "550px",
                    "height": "250px"
                });
                return chartData;
            }

            function loadSpecificChartSimple($canvas, title, color, labelDescription, labels, datas, percentOf, format, maintainAspectRatio) {
                var chartData = {
                    type: "bar",
                    options: {
                        tooltips: {
                            callbacks: {
                                label: function(tooltipItems, data) {
                                console.log(tooltipItems)
                                console.log(data)
                                    var pre = data.datasets[tooltipItems.datasetIndex].label;
                                    if (percentOf !== null){
                                        return pre + ": " + numeral(tooltipItems.yLabel).format(format) + " - " +
                                            numeral(percentOf[tooltipItems.index]).format("0,00.00") + "%";
                                    } else {
                                        return pre + ": " + numeral(tooltipItems.yLabel).format(format);

                                    }
                                }
                            }
                        },
                        scales: {
                            yAxes: [{
                                ticks: {
                                    beginAtZero: false,
                                    callback: function(value, index, values) {
                                        return numeral(value).format(format);
                                    }
                                }
                            }]
                        },
                        title: {
                            display: true,
                            text: title
                        },
                        maintainAspectRatio: maintainAspectRatio
                    },
                    data: {
                        labels: labels,
                        datasets: [
                            {
                                pointRadius: 10,
                                spanGaps: true,
                                lineTension: 0,
                                fill: false,
                                label: labelDescription,
                                backgroundColor : color,
                                data : datas
                            }
                        ]
                    }
                };

               $($canvas).css({
                   "width": "350px",
                   "min-height": "300px"
               });
               var chart = new Chart($canvas, chartData);
            }

            function loadAgeGroupSpecificChart($chartCanvas, $legend , profiles) {
                var groupsLabels = profiles.map(function(e) {
                    return e.ageGroup;
                });

                var title = "Quantidade de eleitores distribuídos por faixa etária";
                var chartData = createSpecificDividedBySexChartData($chartCanvas, $legend, profiles, title, groupsLabels);
                var chart = new Chart($chartCanvas, chartData);
                return chart;
            }

            function loadSchoolingSpecificChart($chartCanvas, $legend, profiles) {

                var schoolingLabels = profiles.map(function(e) {
                    return e.schooling;
                });

                var title = "Quantidade de eleitores distribuídos por nível de escolaridade";
                var chartData = createSpecificDividedBySexChartData($chartCanvas, $legend, profiles, title, schoolingLabels);

                var chart = new Chart($chartCanvas, chartData);
                return chart;
            }

            function loadSexSpecificChart($chartCanvas, $legend, profiles) {
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
                            },
                            tooltips: {
                                callbacks: {
                                    label: function(tooltipItems, data) {
                                        console.log(tooltipItems);
                                        console.log(data);
                                        var pre = data.labels[tooltipItems.index]
                                        var value = data.datasets[0].data[tooltipItems.index];
                                        return pre + ": " + numeral(value).format("0,0");
                                    }
                                }
                            },
                            maintainAspectRatio: false
                        },
                        data: {
                            labels: ["Feminino", "Masculino", "Não Informado"],
                            datasets: [
                                {
                                    data: [dataF, dataM, dataN],
                                    backgroundColor: [
                                        "#ee2929",
                                        "#3572dd",
                                        "#c34fb5"
                                    ],
                                    strokeColor: [
                                        "#e24d4d",
                                        "#4b76c1",
                                        "#a05697"
                                    ]
                                }
                            ]
                        }
                    };

                    $($chartCanvas).css({
                        "width": "550px",
                        "height": "200px"
                    });


                    var chart = new Chart($chartCanvas, chartData);
                    return chart;
            }


            function loadSmooth(){
               $(document).on('click', "#ul-years a, .smooth", function(event){
                   event.preventDefault();

                   $("html, body").animate({
                       scrollTop: $( $.attr(this, "href") ).offset().top
                   }, 500);
               });
            }

            function cleanUrl(){
                    var hash = location.hash.replace('#','');
                    if(hash != ''){
                        location.hash = '';
                    }
            }

            onLoad();
            loadSmooth();
            cleanUrl();
    }
    cityPageModule();

});