$(function() {

  var menuSearchState = {}

  $("#menu_table tbody").on("click", "tr", function(e) {
    var uuid = $(this).find('td:eq(0)').text()
    var menu = menuSearchState[uuid]

    $("#uuid").val(uuid)
    $("#name").val(menu.name)
    $("#ingredients").val(menu.ingredients)
    $("#recipe").val(menu.recipe)
    $("#link").val(menu.link)
  })

  $('#search').click(function() {
    searchMenu()
  })

  $('#submit').click(function() {
    var name = $("#name").val()
    var ingredients = $("#ingredients").val().trim().split(',')
    var recipe = $("#recipe").val()
    var link = $("#link").val()
    var password = $("#password").val()
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
      var result = response.result
      if (result == "ACCESS DENIED") {
        alert("Your password does not match!")
      } else {
        alert("Your Menu has been created (updated) successfully!")
      }
      searchMenu()
    })
    .fail(function(jqXHR, textStatus) {
      alert("Menu create (update) failed")
    })
  })

  $('#delete').click(function() {
    var uuid = $("#uuid").val()
    var password = $("#password").val()
    $.ajax({
        url: '/menu',
        type: 'delete',
        contentType: 'application/json',
        data: JSON.stringify({
          'uuid': uuid,
          'password': password
        })
    })
    .done(function(response) {
      var result = response.result
      if (result == "ACCESS DENIED") {
        alert("Your password does not match!")
      } else {
        alert("Your Menu has been deleted successfully!")
      }
      searchMenu()
    })
    .fail(function(jqXHR, textStatus) {
      alert("Menu delete failed")
    })
  })

  $('#random').click(function() {
    $.ajax({
        url: '/menu/random',
        type: 'post',
        contentType: 'application/json',
        data: JSON.stringify({})
    })
    .done(function(response) {
      var uuid = response.result
      alert("Random menu [" + uuid + "] has been sent!")
    })
    .fail(function(jqXHR, textStatus) {
      alert("Random select failure")
    })
  })

  function searchMenu() {
    $.ajax({
        url: '/menu/view',
        type: 'get',
    })
    .done(function(response) {
      var menuTableBody = $('#menu_table > tbody')
      menuTableBody.empty()
      var menus = response.result
      $.each(menus, function(i, menu) {
        menuSearchState[menu.uuid] = menu
        menuTableBody.append(
          '<tr><td uuid>' + menu.uuid + '</td><td>' + menu.name + '</td><td>' + menu.ingredients.join(', ') + "</td><td>" + menu.recipe + "</td><td>" + menu.link + "</td></tr>"
        )
      })
    })
    .fail(function(jqXHR, textStatus) {
      alert(textStatus)
    })
  }

})
