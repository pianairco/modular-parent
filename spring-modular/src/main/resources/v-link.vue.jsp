<script>
    Vue.component('v-link', {
        template: '<a v-bind:href="href" v-bind:class="{ active: isActive }" v-on:click="go"><slot></slot></a>',
        props: {
            href: {type:String,required: true}
        },
        computed: {
            isActive () {
                return this.href === this.$root.currentRoute || '#' + this.href === this.$root.currentRoute || (this.href === '/' && this.$root.currentRoute === '');
            }
        },
        methods: {go (event) {
                event.preventDefault();
                this.$root.currentRoute = this.href;
                window.history.pushState(null,routes[this.$root.currentRoute],"/#"+this.href);
            }
        }
    });
</script>
