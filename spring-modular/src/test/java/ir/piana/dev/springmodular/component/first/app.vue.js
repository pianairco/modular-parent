Vue.component('first', {
    template: '$template$',
    data: function() {
        return {
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
                .then(function(response) { this.message = response.data; })
        .catch(function(err) { this.message = err; });
        }
    }
});
