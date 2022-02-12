import Vue from "vue";
import VueRouter, { RouteConfig } from "vue-router";

Vue.use(VueRouter);

const routes: RouteConfig[] = [
  {
    path: "/",
    name: "vocadb",
    component: () => import("@/pages/vocadb/views/VocaDB.vue")
  }
];

export const router = new VueRouter({
  base: "/vocadb",
  mode: "history",
  routes: routes
});
