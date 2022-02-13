import Vue from "vue";
import { BootstrapVue, IconsPlugin } from "bootstrap-vue";

import { library } from "@fortawesome/fontawesome-svg-core";
import { FontAwesomeIcon } from "@fortawesome/vue-fontawesome";
import { far } from "@fortawesome/free-regular-svg-icons";
import { fas } from "@fortawesome/free-solid-svg-icons";
import "bootstrap/dist/css/bootstrap.css";
import "bootstrap-vue/dist/bootstrap-vue.css";
import Home from "@/pages/vocadb/views/VocaDB.vue";

Vue.use(BootstrapVue);
Vue.use(IconsPlugin);

library.add(far);
library.add(fas);
Vue.component("FontAwesomeIcon", FontAwesomeIcon);

new Vue({
  render: h => h(Home)
}).$mount("#app");
