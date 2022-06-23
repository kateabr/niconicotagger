// eslint-disable-next-line @typescript-eslint/no-var-requires
const path = require("path");

module.exports = {
  pages: {
    vocadb: {
      title: "NicoNicoTagger (VocaDB)",
      entry: "src/main/typescript/pages/vocadb/main.ts"
    },
    nicovideo: {
      title: "NicoNicoTagger (Nico)",
      entry: "src/main/typescript/pages/nicovideo/main.ts"
    },
    events: {
      title: "NicoNicoTagger (Events)",
      entry: "src/main/typescript/pages/events/main.ts"
    },
    login: {
      title: "Login",
      entry: "src/main/typescript/pages/login/main.ts"
    },
    index: {
      title: "Login",
      entry: "src/main/typescript/pages/login/main.ts"
    }
  },

  configureWebpack: {
    resolve: {
      alias: {
        "@": path.resolve(__dirname, "src/main/typescript")
      }
    }
  },

  outputDir: "target/spa",
  lintOnSave: false,

  css: {
    sourceMap: true
  },

  devServer: {
    proxy: "http://localhost:8080"
  }
};
