module.exports = {
  testEnvironment: 'jsdom',
  setupFilesAfterSetup: ['./__tests__/setup.js'],
  moduleNameMapper: {
    '^../../api/(.*)$': '<rootDir>/api/$1',
    '^../../utils/(.*)$': '<rootDir>/utils/$1',
    '^../api/(.*)$': '<rootDir>/api/$1',
    '^../utils/(.*)$': '<rootDir>/utils/$1'
  },
  transform: {
    '^.+\\.js$': 'babel-jest'
  },
  testMatch: [
    '**/__tests__/**/*.test.js'
  ],
  testPathPattern: '__tests__',
  collectCoverageFrom: [
    'api/**/*.js',
    'pages/**/*.js',
    'utils/**/*.js',
    '!pages/**/*.wxml',
    '!pages/**/*.wxss',
    '!pages/**/*.json'
  ],
  coverageDirectory: './coverage',
  coverageReporters: ['json', 'lcov', 'text', 'html'],
  verbose: true
};