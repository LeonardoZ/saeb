$(function(){

    var dataTableTranslated = {
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

    function centerModal() {
            function reposition() {
                var modal = $(this),
                    dialog = modal.find('.modal-dialog');
                modal.css('display', 'block');

                // Dividing by two centers the modal exactly, but dividing by three
                // or four works better for larger screens.
                dialog.css("margin-top", Math.max(0, ($(window).height() - dialog.height()) / 2));
            }
            // Reposition when a modal is shown
            $('.modal').on('show.bs.modal', reposition);
            // Reposition when the window is resized
            $(window).on('resize', function() {
                $('.modal:visible').each(reposition);
            });
    }

    function usersModule() {
        $('#users').DataTable({
             "language": dataTableTranslated
        });

        $('#users-inactive').DataTable({
             "language": dataTableTranslated
        });
    }

    function userProfileModule() {

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
            });

         }


        initState();
        orderedListConfigure();
        upButtonConfigure();
        downButtonConfigure();
        saveNewOrder();
    }

    function uploadModule() {
         $('#fileupload').fileupload({
              dataType: 'json',
              add: function (e,data) {
                $('#progress-bar').css('width', '0%');
                $('#progress').show();
                data.submit();
              },
              progressall: function (e, data) {
                var progress = parseInt(data.loaded / data.total * 100, 10) + '%';
                $('#progress-bar').css('width', progress);
              },
              done: function (e, data) {
                $.each(data.files, function (index, file) {
                  $('<p/>').text(file.name).appendTo(document.body);
                });
                $('#progress').fadeOut();
              }
         });

         $('#fileupload').bind('fileuploadprogress', function (e, data) {
                // Log the current bitrate for this upload:
                console.log(data);
         });
    }

    function secUploadModule() {
        var url = $("#fileupload").attr("data-url");
        var uploadButton = $('<button/>')
            .addClass('btn btn-primary')
            .prop('disabled', true)
            .text('Processando... Não feche essa janela!')
            .on('click', function () {
                var $this = $(this),
                    data = $this.data();
                $this
                    .off('click')
                    .text('Cancelar')
                    .on('click', function () {
                        $this.remove();
                        data.abort();

                    });
                data.submit().always(function () {
                    $this.remove();
                    $("#success").html("<p>Envio de arquivo realizado com sucesso. Em breve será processado.</p>");
                });
            });
        $('#fileupload').fileupload({
            url: url,
            dataType: 'json',
            paramName: 'document',
            autoUpload: false,
            acceptFileTypes: /(\.|\/)(txt)$/i,
            maxFileSize: 100000000
        }).on('fileuploadadd', function (e, data) {
            data.context = $('<div/>').appendTo('#files');
            $.each(data.files, function (index, file) {
                var node = $('<p/>')
                        .append($('<span/>').text(file.name));
                if (!index) {
                    node
                        .append('<br>')
                        .append(uploadButton.clone(true).data(data));
                }
                node.appendTo(data.context);
            });
        }).on('fileuploadprocessalways', function (e, data) {
            var index = data.index,
                file = data.files[index],
                node = $(data.context.children()[index]);
            if (file.preview) {
                node
                    .prepend('<br>')
                    .prepend(file.preview);
            }
            if (file.error) {
                node
                    .append('<br>')
                    .append($('<span class="text-danger"/>').text(file.error));
            }
            if (index + 1 === data.files.length) {
                data.context.find('button')
                    .text('Enviar')
                    .prop('disabled', !!data.files.error);
            }
        }).on('fileuploadprogressall', function (e, data) {
            var progress = parseInt(data.loaded / data.total * 100, 10);
            $('#progress .progress-bar').css(
                'width',
                progress + '%'
            );
        }).on('fileuploaddone', function (e, data) {
            $.each(data.result.files, function (index, file) {
                if (file.url) {
                    var link = $('<a>')
                        .attr('target', '_blank')
                        .prop('href', file.url);
                    $(data.context.children()[index])
                        .wrap(link);

                } else if (file.error) {
                    var error = $('<span class="text-danger"/>').text(file.error);
                    $(data.context.children()[index])
                        .append('<br>')
                        .append(error);
                }
            });
        }).on('fileuploadfail', function (e, data) {
            $.each(data.files, function (index) {
                var error = $('<span class="text-danger"/>').text('Falha ao enviar arquivo.');
                $(data.context.children()[index])
                    .append('<br>')
                    .append(error);
            });
        }).prop('disabled', !$.support.fileInput)
            .parent().addClass($.support.fileInput ? undefined : 'disabled');
    }


    function mainModule() {
        $("#uploads-table").DataTable({
            language: dataTableTranslated
        });
    }

    centerModal();
    usersModule();
    schoolingModule();
    secUploadModule();
    mainModule();
});