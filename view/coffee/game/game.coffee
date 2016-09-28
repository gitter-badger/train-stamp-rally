$(document).ready ->
  new Vue
    el: '#game'
    mixins: [formatter]
    data:
      missionId: 0
      game: {}
      diagrams: []
    methods:
      setMission: ->
        @missionId = fromURLParameter(location.search.slice(1)).mission
        if !@missionId
          location.href = '/game/index.html'
      getGame: ->
        API.getJSON "/api/game/#{@missionId}", (json) =>
          @game = json
          @getDiagrams()
      getDiagrams: ->
        API.getJSON "/api/diagrams?station=#{@game.station.id}&time=#{@timeFormatAPI(@game.time)}", (json) =>
          @diagrams = json
      openModal: (diagram) ->
        new Vue(modalVue(diagram.train.id, @game.station.id))
        $('#trainModal').modal('show')
    ready: ->
      @setMission()
      @getGame()
  modalVue = (trainId, stationId) ->
    el: '#trainModal'
    mixins: [formatter]
    data:
      train: {}
    methods:
      getTrain: (id) ->
        API.getJSON "/api/train/#{id}", (json) =>
          stops = _.dropWhile json.stops, (stop) =>
            stop.station.id != stationId
          @train = json
          @train.stops = stops
    ready: ->
      @getTrain(trainId)
