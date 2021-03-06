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
            axios.post('/action', this.user, {headers: {"action": "FirstAction", "activity": "x"}})
                .then(function(response) {
                    this.message = response.data;
                    this.storeState.setState('twoComponent', {user: this.user})
                })
        .catch(function(err) { this.message = err; });
        }
    }
});
