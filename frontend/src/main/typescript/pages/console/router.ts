import Vue from "vue";
import VueRouter, { RouteConfig } from "vue-router";

Vue.use(VueRouter);

const routes: RouteConfig[] = [
  {
    path: "/:browseMode/:userQuery",
    name: "console-full",
    props: true,
    component: () => import("@/pages/console/views/QueryConsole.vue")
  },
  {
    path: "/:browseMode",
    name: "console-mode",
    props: true,
    component: () => import("@/pages/console/views/QueryConsole.vue")
  },
  {
    path: "/",
    name: "console",
    props: {
      browseMode: "songs"
    },
    component: () => import("@/pages/console/views/QueryConsole.vue")
  }
];

export const router = new VueRouter({
  base: "/console",
  mode: "history",
  routes: routes
});
