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
    var search = $("#searchForm").val()
    searchMenu(search)
  })

  $('#submit').click(function() {
    var name = $("#name").val()
    var ingredients = $("#ingredients").val().trim().split(',')
    var recipe = $("#recipe").val()
    var link = $("#link").val()
    var password = $("#password").val()
    $.ajax({
      url: '/v1/menu',
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

      var menuTableBody = $('#menu_table > tbody')
      menuTableBody.empty()
    })
    .fail(function(jqXHR, textStatus) {
      alert("Menu create (update) failed")
    })
  })

  $('#delete').click(function() {
    var uuid = $("#uuid").val()
    var password = $("#password").val()
    $.ajax({
      url: '/v1/menu',
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

      var menuTableBody = $('#menu_table > tbody')
      menuTableBody.empty()
    })
    .fail(function(jqXHR, textStatus) {
      alert("Menu delete failed")
    })
  })

  $('#random').click(function() {
    $.ajax({
      url: '/v1/menu/view',
      type: 'get',
      data: {
        name: menuName,
      }
    })
    .done(function(response) {
      var menuTableBody = $('#menu_table > tbody')
      menuTableBody.empty()
      var menus = response.result
      var randomMenu = menus[Math.floor(Math.random() * menus.length)];
			$.ajax({
				url: '/v1/menu/random',
				type: 'post',
				contentType: 'application/json',
				data: JSON.stringify({ 'uuid': randomMenu.uuid })
			})
			.done(function(response) {
				var uuid = response.result
				alert("Random menu [" + uuid + "] has been sent!")
			})
			.fail(function(jqXHR, textStatus) {
				alert("Random select failure")
			})
    })
    .fail(function(jqXHR, textStatus) {
      alert(textStatus)
    })
  })

  function searchMenu(menuName) {
    $.ajax({
      url: '/v1/menu/view',
      type: 'get',
      data: {
        name: menuName,
      }
    })
    .done(function(response) {
      var menuTableBody = $('#menu_table > tbody')
      menuTableBody.empty()
      var menus = response.result
      $.each(menus, function(i, menu) {
        menuSearchState[menu.uuid] = menu
        menuTableBody.append(
          '<tr><td uuid>' + menu.uuid + '</td><td>' + menu.name + '</td><td>' + menu.ingredients.join(', ') + "</td><td>" + menu.recipe + "</td><td><a href=\"" + menu.link + "\">" + menu.link + "</a></td></tr>"
        )
      })
    })
    .fail(function(jqXHR, textStatus) {
      alert(textStatus)
    })
  }

})
