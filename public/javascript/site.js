$(function(){
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

    /**
     * Declare Modules
     **/
    function frontPageModule() {
        var $searchSelect = $("#cityCode");
        load($searchSelect);
    }

    function baseAjaxRequest(endpoint, postData, successCallback) {
        baseAjax("html", endpoint, postData, successCallback);
    }


    function baseAjaxRequestJson(endpoint, postData, successCallback) {
        baseAjax("json", endpoint, postData, successCallback);
    }

    function baseAjax(dataType, endpoint, postData, successCallback) {
            $.ajax({ url: endpoint,
                dataType: dataType,
                cache: false,
                contentType: "application/json",
                type: "POST",
                data: JSON.stringify(postData),
                success: successCallback
            });
    }


    function load($searchSelect) {
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
          escapeMarkup: function (markup) { return markup; },
          minimumInputLength: 1,
          templateResult: function (option) {
              return "<div>" + option.name +" - "+ option.state + "</div>";
          },
          templateSelection: function (option) {
              return option.text === "" ? option.name +" - "+ option.state : option.text;
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

    function cityComparisonModule() {
        var $firstCityName = $("#firstCity-input");
        var $secondCityName = $("#secondCity-input");
        var $yearSelect = $("#year-select");
        var $btnCompare = $("#btn-compare");
        var cityOneCode = "";
        var cityTwoCode = "";
        var year = "";
        var $comparisonsRow = $("#comparisons-row");
        var selectedOne = false;
        var selectedTwo = false;

        function compare() {
            cityOneCode = $firstCityName.val();
            cityTwoCode = $secondCityName.val();
            year = $yearSelect.val();

            baseAjaxRequest("/comparison", {
                "year": year,
                "codeOfCityOne": cityOneCode,
                "codeOfCityTwo": cityTwoCode
            }, compareCallback);
        }

        function compareCallback(data) {
            $comparisonsRow.empty();
            $comparisonsRow.append($(data));

            roundUtil();
            loadCharts();
        }

        function cityOneSelected(e) {
            selectedOne = true;
            checkCompareButton();
        }

        function cityTwoSelected(e) {
            selectedTwo = true;
            checkCompareButton();
        }

        function cityOneUnselected(e) {
            selectedOne = false;
            checkCompareButton();
        }

        function cityTwoUnselected(e) {
            selectedTwo = false;
            checkCompareButton();
        }

        function checkCompareButton(){
            if (selectedOne === true && selectedTwo === true) {
                $btnCompare.removeAttr("disabled");
            } else {
                $btnCompare.attr("disabled");
            }
        }

        function loadCharts(){
            var $inputs = $("input[type='hidden']");
            var $inputYear = $($inputs[0]);
            var $inputCityOne = $($inputs[1]);
            var $inputCityTwo = $($inputs[2]);

            baseAjaxRequestJson("/comparison/of/schooling", {
                "year": $inputYear.val(),
                "codeOfCityOne": $inputCityOne.val(),
                "codeOfCityTwo": $inputCityTwo.val()
            }, schoolingChartCallback);

            baseAjaxRequestJson("/comparison/of/agegroup", {
                "year": $inputYear.val(),
                "codeOfCityOne": $inputCityOne.val(),
                "codeOfCityTwo": $inputCityTwo.val()
            }, ageGroupChartCallback);
        }

        function schoolingChartCallback(data) {
            var labels = data.comparisons.cityOne.map(function(e) {
                return e.level;
            });
            var nameOne = data.comparisons.cityNameOne;
            var nameTwo = data.comparisons.cityNameTwo;
             data.comparisons.cityNameTwo
            createChart(
             $("#chart-schooling"),
             data.comparisons,
             "Escolaridade: " + nameOne + " - " + nameTwo,
             labels,
             nameOne,
             nameTwo)
        }

        function ageGroupChartCallback(data) {
            var labels = data.comparisons.cityOne.map(function(e) {
                return e.group;
            });
            var nameOne = data.comparisons.cityNameOne;
            var nameTwo = data.comparisons.cityNameTwo;
             data.comparisons.cityNameTwo
            createChart(
             $("#chart-age-group"),
             data.comparisons,
             "Faixa Etária: " + nameOne + " - " + nameTwo,
             labels,
             nameOne,
             nameTwo)
        }

        function createChart($chartCanvas, comparisons, title, labels, name1, name2) {
                var dataOne = comparisons.cityOne.map(function(e) {
                    return e.percentOfTotal;
                });

                var dataTwo = comparisons.cityTwo.map(function(e) {
                    return e.percentOfTotal;
                });

                var chartData = {
                    type: "bar",
                    options: {
                        tooltips: {
                            callbacks: {
                                label: function(tooltipItems, data) {
                                    var pre = data.datasets[tooltipItems.datasetIndex].label;
                                    return pre + ": " + numeral(tooltipItems.yLabel).format("0,00.00") + "%";
                                }
                            }
                        },
                        scales: {
                            yAxes: [{
                                ticks: {
                                    beginAtZero: true,
                                    callback: function(value, index, values) {
                                        return numeral(value).format("0.0") + "%";
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
                                label: name1,
                                backgroundColor : "#00a6ed",
                                data : dataOne
                            },{
                                label: name2,
                                backgroundColor : "#7fb800",
                                data : dataTwo
                            }
                        ]
                    }
                };

                $($chartCanvas).css({
                    "width": 750,
                    "height": 400
                });
                return new Chart($chartCanvas, chartData);
            }

        $firstCityName.on("select2:select", cityOneSelected);
        $secondCityName.on("select2:select", cityTwoSelected);
        $firstCityName.on("select2:unselect", cityOneUnselected);
        $secondCityName.on("select2:unselect", cityTwoUnselected);

        load($firstCityName);
        load($secondCityName);

        $btnCompare.on("click", function(evt) {
            compare();
        });

    }

    function statePageModule(){
        $("#cities-table").DataTable({
            language: dataTableLanguage
        });
    }

    function rankingPagesModules(){
        var $contentTabs = $("div.ranking-content");
        $.each($contentTabs, function(key, value) {
            var $child = $($(value).children()[0]);
            $child.toggleClass("in active");
        });
        roundUtil();
    }

    function numeralConfig() {
        numeral.language('pt-br');
        console.log(numeral.language);
    }


     function loadSmooth(){
         $(document).on('click', "#category-list a, .smooth", function(event){
         event.preventDefault();

         $("html, body").animate({
                scrollTop: $($.attr(this, "href")).offset().top
             },
             500);
         });
     }

     function cleanUrl(){
         var hash = location.hash.replace("#", "");
         if(hash != ""){
            location.hash = "";
         }
     }

     loadSmooth();
     cleanUrl();

    // Load modules
    numeralConfig();
    frontPageModule();
    cityComparisonModule();
    statePageModule();
    rankingPagesModules();

});

