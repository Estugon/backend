function clearFlash(){
  $("#flash").remove()
}

function addFlash(type, message){
  if($("#flash").length == 0){
    $("#content").prepend('<div id="flash" style="margin: 0 -10px;"></div>')
  }

  if(type == "notice"){
    $("#flash").append('<div class="ui-widget"><div class="ui-state-highlight ui-corner-all" style="margin: 0.6em 0; padding: 0 .7em;"><p><span class="ui-icon ui-icon-info" style="float: left; margin-right: .3em;"></span><strong>Hinweis:</strong>'+message+'</p></div></div>')
  }else{
    $("#flash").append('<div class="ui-widget"><div class="ui-state-error ui-corner-all" style="margin: 0.6em 0; padding: 0 .7em;"><p><span class="ui-icon ui-icon-alert" style="float: left; margin-right: .3em;"></span><strong>Fehler:</strong> '+message+'</p></div></div>')
  }
}

function refresh_counter(input,counter){
  var length = input.attr("value").length;
  var color = "green";
  counter.html(length);
  if(length > 25){       
    color = "red";
  }
  counter.css("color",color);
}

function refresh_counter_confirm(input,counter,confirmobj,confirmmsg){
  var length = input.attr("value").length;
  if(length > 25){
    confirmobj.unbind("click");
    confirmobj.bind("click", function() {
      if (!confirm(confirmmsg)) return false; return true;
    });
  } else {
    confirmobj.unbind("click");
    confirmobj.bind("click", function() {
      return true;
    });
  }
}


