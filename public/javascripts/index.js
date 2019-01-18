$(function() {

  $(".test").click(function() {
    $.get("/menu/view", function(data, status) {
      console.log(JSON.stringify(data));
    });
  });
});
