$(function(){

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

    function usersModule() {
        $('#users').DataTable({
             "language":
                 {
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
        });
    }

    function schoolingModule(){
        var $orderedList = $("#ordered-list");

        var $btnUp = $("#btn-up");
        var $btnDown = $("#btn-down");
        var $btnSave = $("#btn-save-order");

        var $orderedSelectedItem = null;

        function initState() {
            $btnUp.attr("disabled", "disabled");
            $btnDown.attr("disabled", "disabled");
        }

         function orderedListConfigure() {
             $orderedList.delegate("a", "click", function(e) {
                 var element = e.target;

                 $("#ordered-list > a.active").toggleClass("active");
                 $orderedSelectedItem = $(element);
                 $orderedSelectedItem.toggleClass("active");
                 $btnUp.removeAttr("disabled");
                 $btnDown.removeAttr("disabled");
             });
         }

         function downButtonConfigure() {
            $btnDown.on("click", function() {
                $($orderedSelectedItem).insertAfter($($orderedSelectedItem).next());
            });
         }

         function upButtonConfigure() {
            $btnUp.on("click", function() {
                $($orderedSelectedItem).insertBefore($($orderedSelectedItem).prev());

            });
         }

         function saveNewOrder() {
            $btnSave.on("click", function(){
              var schoolingOrder = [];
              $.each($orderedList.children(), function(i, ele) {
                  var id = $(ele).attr("data-id");
                  var values = {
                      "id" : Number.parseInt(id),
                      "index" : Number.parseInt(i) + 1
                  };
                  console.log(values);
                  schoolingOrder.push(values);
              });

              var url =  "/admin/schooling/classification";
              baseAjaxRequest(url, schoolingOrder, function(data) {
                  if (data["inserted"] != null) {
                    window.location.reload();
                  }
              });
              console.log(schoolingOrder.toString());
            });

         }


        initState();
        orderedListConfigure();
        upButtonConfigure();
        downButtonConfigure();
        saveNewOrder();
    }


    usersModule();
    schoolingModule();
});