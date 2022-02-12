import Vue from "vue";
import VueRouter, { RouteConfig } from "vue-router";

Vue.use(VueRouter);

const routes: RouteConfig[] = [
  {
    path: "/",
    name: "nicovideo",
    component: () => import("@/pages/nicovideo/views/Nicovideo.vue")
  }
];

export const router = new VueRouter({
  base: "/vocadb",
  mode: "history",
  routes: routes
});
