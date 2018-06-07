const exec = require('cordova/exec');

const PLUGIN_NAME = 'GlobostoreAutoUpdate'

/**
 * This represents the mobile device, and provides properties for inspecting the model, version, UUID of the
 * phone, etc.
 * @constructor
 */
let GlobostoreAutoUpdate = {
    check: (phrase, cb) => exec(cb, null, PLUGIN_NAME, 'check', [phrase]),
};
    
module.exports = GlobostoreAutoUpdate;

