$(document).ready ->
  new Vue
    el: '#creatorNav'
    data:
      categories: [
        {name: 'mission', role: 1},
        {name: 'line', role: 0},
        {name: 'diagram', role: 0},
        {name: 'validator', role: 0}
      ]
      category:  ''
    methods:
      filterRole: ->
        API.getJSON '/api/account', (account) =>
          @categories = @categories.filter (cat) -> account.role == 0 or cat.role == account.role
      parseCategory: ->
        @category = location.pathname.split('/')[2]
      camelCase: (str) ->
        str.charAt(0).toUpperCase() + str.substring(1)
    ready: ->
      @parseCategory()
      @filterRole()
