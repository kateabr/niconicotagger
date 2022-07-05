import Vue from "vue";
import VueRouter, { RouteConfig } from "vue-router";

Vue.use(VueRouter);

const routes: RouteConfig[] = [
  {
    path: "/",
    name: "events",
    props: {
      browseMode: "vocadb"
    },
    component: () => import("@/pages/events/views/Events.vue")
  },
  {
    path: "/:browseMode",
    name: "events-mode",
    props: true,
    component: () => import("@/pages/events/views/Events.vue")
  },
  {
    path: "/:browseMode/:targName",
    name: "events-full",
    props: true,
    component: () => import("@/pages/events/views/Events.vue")
  }
];

export const router = new VueRouter({
  base: "/events",
  mode: "history",
  routes: routes
});
