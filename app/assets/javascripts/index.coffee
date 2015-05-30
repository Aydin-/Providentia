$ ->
  ws = new WebSocket $("body").data("ws-url")
  ws.onmessage = (event) ->
    message = JSON.parse event.data
    switch message.type
      when "stockhistory"
        populateStockHistory(message)
      when "stockupdate"
        updateStockChart(message)
      when "fundupdate"
        updateFundChange(message)
      when "progressbar"
        updateProgressBar(message)
      else
        console.log(message)

  $("#addsymbolform").submit (event) ->
    event.preventDefault()
    $("#stocks ul").empty()
    # send the message to watch the stock
    ws.send(JSON.stringify({symbol: $("#addsymboltext").val()}))

getPricesFromArray = (data) ->
  (v[1] for v in data)

getChartArray = (data) ->
  ([i, v] for v, i in data)

getChartOptions = (data) ->
  series:
    shadowSize: 0
  yaxis:
    min: getAxisMin(data)
    max: getAxisMax(data)
  xaxis:
    show: false

getAxisMin = (data) ->
  Math.min.apply(Math, data) * 0.9

getAxisMax = (data) ->
  Math.max.apply(Math, data) * 1.1

populateStockHistory = (message) ->
  chart = $("<div>").addClass("chart").prop("id", message.symbol)
  chartHolder = $("<div>").addClass("chart-holder").append(chart)
  chartHolder.append($("<p>").text("Realtime percent change " + message.percentage))
  detailsHolder = $("<div>").addClass("details-holder")
  flipper = $("<div>").addClass("flipper").append(chartHolder).prepend(detailsHolder).attr("data-content", message.symbol)
  flipContainer = $("<div>").addClass("flip-container").append(flipper).click (event) ->
    handleFlip($(this))
  li = $("<li>").prepend(flipContainer)

  if(message.percentage.substring(1, message.percentage.length-1) > 0.0)
    li = li.addClass("red-background")
  else
    li = li.addClass("green-background")
  $("#stocks ul").prepend(li)
  plot = chart.plot([getChartArray(message.history)], getChartOptions(message.history)).data("plot")

updateProgressBar = (message) ->
  $('.bar').text(message.progressMessage + " " + message.totalPercentage + "%");
  $('.bar').width(message.totalPercentage + "%")

updateFundChange = (message) ->
  $("#fund").empty()
  $("#fund").prepend($("<h4>").text(message.percentage))
  $('.bar').width("100%")
  $('.bar').text("Estimation complete")
  $('.progress').removeClass('active');


updateStockChart = (message) ->
  if ($("#" + message.symbol).size() > 0)
    plot = $("#" + message.symbol).data("plot")
    data = getPricesFromArray(plot.getData()[0].data)
    data.shift()

    data.push(message.price)
    plot.setData([getChartArray(data)])
    # update the yaxes if either the min or max is now out of the acceptable range
    yaxes = plot.getOptions().yaxes[0]
    if ((getAxisMin(data) < yaxes.min) || (getAxisMax(data) > yaxes.max))
      # reseting yaxes
      yaxes.min = getAxisMin(data)
      yaxes.max = getAxisMax(data)
      plot.setupGrid()
    # redraw the chart
    plot.draw()

handleFlip = (container) ->
  if (container.hasClass("flipped"))
    container.removeClass("flipped")
    container.find(".details-holder").empty()
  else
    container.addClass("flipped")
    # fetch stock details and tweet
    $.ajax
      url: "/sentiment/" + container.children(".flipper").attr("data-content")
      dataType: "json"
      context: container
      success: (data) ->
        detailsHolder = $(this).find(".details-holder")
        detailsHolder.empty()
        switch data.label
          when "pos"
            detailsHolder.append($("<h4>").text("The tweets say BUY!"))
            detailsHolder.append($("<img>").attr("src", "/assets/images/buy.png"))
          when "neg"
            detailsHolder.append($("<h4>").text("The tweets say SELL!"))
            detailsHolder.append($("<img>").attr("src", "/assets/images/sell.png"))
          else
            detailsHolder.append($("<h4>").text("The tweets say HOLD!"))
            detailsHolder.append($("<img>").attr("src", "/assets/images/hold.png"))
      error: (jqXHR, textStatus, error) ->
        detailsHolder = $(this).find(".details-holder")
        detailsHolder.empty()
        detailsHolder.append($("<h4>").text("Error: " + JSON.parse(jqXHR.responseText).error))
    # display loading info
    detailsHolder = container.find(".details-holder")
    detailsHolder.append($("<h4>").text("Determing whether you should buy or sell based on the sentiment of recent tweets..."))
    detailsHolder.append($("<div>").addClass("progress progress-striped active").append($("<div>").addClass("bar").css("width", "100%")))