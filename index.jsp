<%-- Created by IntelliJ IDEA. --%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
  <head>
    <script src="//ajax.googleapis.com/ajax/libs/jquery/3.1.0/jquery.min.js"></script>
    <title>Search city</title>
  </head>
  <body>


  <form method="get" action="main">
  <div style="width:100%;" align="center">
      <div style="width:350px !important;" align="center">
        <input placeholder="Откуда" name="from" type="text" id="from" style="height:40px;font-family:Arial;width:350px !important; font-size: 20px; text-align: center">
        <div id="resultfrom" style="position:absolute;width:350px !important;"></div>
      </div><br><br>

      <div style="width:350px !important;" align="center">
        <input placeholder="Куда" name="to" type="text" id="to" style="height:40px;font-family:Arial;width:350px !important; font-size: 20px; text-align: center">
      <div id="resultto" style="position:absolute;width:350px !important;"></div>
    </div><br><br>

      <div style="width:350px !important;" align="center">
        <input placeholder="Дата" name="date" type="text" id="date" style="height:40px;font-family:Arial;width:350px !important; font-size: 20px; text-align: center">
    </div><br><br>

      <div style="width:350px !important;" align="center">
        <input placeholder="Макс. цена" name="maxprice" type="text" id="maxPrice" style="height:40px;font-family:Arial;width:350px !important; font-size: 20px; text-align: center">
      </div>

    <input type="submit" name="submit" value="Send" style="width:300px; height:50px; margin:40px; border-radius: 5px;">
  </div>
  </form>

  <script>
var lastValFROM;
var newValFROM;

var lastValTO;
var newValTO;


$("body").click(function () {
    $("#resultfrom").html("");
    $("#resultto").html("");
});

    function searchCity() {
        lastValFROM = newValFROM;
        newValFROM = $('#from').val().trim();


        lastValTO = newValTO;
        newValTO = $('#to').val().trim();


        if (newValFROM != lastValFROM) {

            if (newValFROM.length >= 2) {
                $.ajax({
                    type: "POST",
                    url: "autocomplete",
                    data: encodeURIComponent(newValFROM + "//from"),
                    success: function (result) {
                        $("#resultfrom").html(result);
                    }
                });
            }else{
                $("#resultfrom").html("");
            }
        }


        if(newValTO!=lastValTO){

            if(newValTO.length >= 2){
                $.ajax({
                    type: "POST",
                    url: "autocomplete",
                    data: encodeURIComponent(newValTO + "//to"),
                    success: function (result) {
                        $("#resultto").html(result);
                    }
                });
            } else {
                $("#resultto").html("");
            }

        }

    }

    setInterval(searchCity, 1500);



  </script>




  </body>
</html>