Vue.component('$app$', {
    template: '$template$',
    data: function() {
        return {
            storeState: store.state,
            user: {
                firstName: '',
                lastName: ''
            },
            message: 'Hello To Spring Vue'
        }
    },
    methods: {
        x: function () {
            axios.post('/action', this.user, {headers: {"action": "TwoAction", "activity": "x"}})
                .then(function(response) { this.message = response.data; })
        .catch(function(err) { this.message = err; });
        }
    }
});
