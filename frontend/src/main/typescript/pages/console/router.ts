import Vue from "vue";
import VueRouter, { RouteConfig } from "vue-router";
import { serviceName } from "@/constants";

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

router.beforeEach((to, from, next) => {
  const mode = to.params["browseMode"];
  switch (mode) {
    case "songs":
    case undefined:
      document.title = `${serviceName} | Query console (Artists)`;
      break;
    case "artists":
      document.title = `${serviceName} | Query console (Songs)`;
      break;
    default:
      document.title = `${serviceName} | Query console (${mode})`;
      break;
  }
  next();
});
