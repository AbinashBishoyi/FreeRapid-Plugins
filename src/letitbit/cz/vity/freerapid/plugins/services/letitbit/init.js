var __outParams = {};
var __dummyElement = {};
var window = {};
var document = {
    createElement: function () {
        return __dummyElement;
    }
};
function $(data) {
    if (data === document) {
        return {
            ready: function () {
            },
            on: function () {
            }
        }
    }
    else if (data === __dummyElement) {
        return {
            attr: function (param) {
                __outParams[param.name] = param.value;
            }
        }
    }
    else if (typeof(data) === "string") {
        if (data.indexOf("#jsprotect_") === 0) {
            return {
                closest: function () {
                    return {
                        append: function () {
                        },
                        find: function () {
                            return {
                                remove: function () {
                                }
                            }
                        },
                        attr: function () {
                        }
                    }
                }
            }
        } else {
            return {
                __id: data.substring(1),
                attr: function () {
                    return __params[this.__id];
                }
            }
        }
    }
    return undefined;
}
$.cookie = function (name) {
    return __cookies[name];
};
function setTimeout() {
}