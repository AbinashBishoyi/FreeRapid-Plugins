document = {
    elements: {},

    getElementById: function (id) {
        var element = this.elements[id];
        if (element === undefined) {
            element = {
                addEventListener: function (s, f) {
                    f();
                }
            };
            this.elements[id] = element;
        }
        return element;
    }
};

window = {};