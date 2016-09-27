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
                   }

    }

    /**
     * Declare Modules
     **/
    function frontPageModule() {
        var $searchSelect = $("#cityCode");
        load($searchSelect);
    }

    function baseAjaxRequest(endpoint, postData, successCallback) {
            $.ajax({ url: endpoint,
                dataType: "html",
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

    function cityComparisonModule() {
        var $firstCityName = $("#firstCity-input");
        var $secondCityName = $("#secondCity-input");
        var $yearSelect = $("#year-select");
        var $btnCompare = $("#btn-compare");
        var cityOneCode = "";
        var cityTwoCode = "";
        var year = "";
        var formatInt = '0,0';
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

            $.each($(".round-int"), function(key, value) {
                var $value = $(value);
                var numValue = $value.html();
                $value.text(numeral(numValue).format(formatInt));
            });

            $.each($(".round-pct"), function(key, value) {
                var $value = $(value);
                var numValue = $value.html();
                $value.text(numeral(numValue).format(formatPercent));
            });
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

    // Load modules
    frontPageModule();
    cityComparisonModule();
    statePageModule();
});

