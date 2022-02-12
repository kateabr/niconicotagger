import Vue from "vue";
import { BootstrapVue, IconsPlugin } from "bootstrap-vue";

import "bootstrap/dist/css/bootstrap.css";
import "bootstrap-vue/dist/bootstrap-vue.css";
import Home from "@/pages/nicovideo/views/Nicovideo.vue";

Vue.use(BootstrapVue);
Vue.use(IconsPlugin);

new Vue({
  render: h => h(Home)
}).$mount("#app");
