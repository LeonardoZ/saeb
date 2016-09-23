$(function(){

    /**
     * Declare Modules
     **/
    function frontPageModule() {
        var $searchSelect = $("#cityCode");
        load($searchSelect);
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

    function cityComparisonModule() {
        var $firstCityName = $("#firstCity-input");
        var $secondCityName = $("#secondCity-input");
        load($firstCityName);
        load($secondCityName);
    }

    // Load modules
    frontPageModule();
    cityComparisonModule();
});

