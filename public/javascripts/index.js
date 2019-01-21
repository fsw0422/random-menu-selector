$(function() {

  $('#search').click(function() {
    $.ajax({
        url: '/menu/view',
        type: 'get',
    })
    .done(function(response) {
      console.log(JSON.stringify(response));
    })
    .fail(function(jqXHR, textStatus) {
      console.log(textStatus);
    });
  });

  $('#submit').click(function() {
    var name = $("#name").val();
    var ingredients = $("#ingredients").val().trim().split(',');
    var recipe = $("#recipe").val();
    var link = $("#link").val();
    var password = $("#password").val();
    $.ajax({
        url: '/menu',
        type: 'post',
        contentType: 'application/json',
        data: JSON.stringify({
          'name': name,
          'ingredients': ingredients,
          'recipe': recipe,
          'link': link,
          'selectedCount': 0,
          'password': password
        })
    })
    .done(function(response) {
      console.log(JSON.stringify(response));
    })
    .fail(function(jqXHR, textStatus) {
      console.log(textStatus);
    });
  });

});
