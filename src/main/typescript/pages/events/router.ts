import Vue from "vue";
import VueRouter, { RouteConfig } from "vue-router";

Vue.use(VueRouter);

const routes: RouteConfig[] = [
  {
    path: "/",
    name: "events",
    component: () => import("@/pages/events/views/Events.vue")
  }
];

export const router = new VueRouter({
  base: "/events",
  mode: "history",
  routes: routes
});
