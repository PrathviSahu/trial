module.exports = {
  webpack: {
    configure: (webpackConfig, { env, paths }) => {
      // Handle face-api.js fs module issue
      webpackConfig.resolve.fallback = {
        ...webpackConfig.resolve.fallback,
        "fs": false,
        "path": false,
        "os": false
      };

      // Disable ESLint during build
      webpackConfig.module.rules = webpackConfig.module.rules.map(rule => {
        if (rule.use && rule.use.includes('eslint-loader')) {
          return {
            ...rule,
            use: rule.use.map(useRule => {
              if (useRule.loader && useRule.loader.includes('eslint-loader')) {
                return {
                  ...useRule,
                  options: {
                    ...useRule.options,
                    emitWarning: false,
                    failOnError: false,
                    failOnWarning: false
                  }
                };
              }
              return useRule;
            })
          };
        }
        return rule;
      });

      return webpackConfig;
    }
  }
};
