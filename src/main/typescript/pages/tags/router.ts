import Vue from "vue";
import VueRouter, { RouteConfig } from "vue-router";

Vue.use(VueRouter);

const routes: RouteConfig[] = [
  {
    path: "/:browseMode",
    name: "tags",
    props: true,
    component: () => import("@/pages/tags/views/Tags.vue")
  },
  {
    path: "/",
    name: "tags",
    props: {
      browseMode: "activity-entries"
    },
    component: () => import("@/pages/tags/views/Tags.vue")
  }
];

export const router = new VueRouter({
  base: "/tags",
  mode: "history",
  routes: routes
});
