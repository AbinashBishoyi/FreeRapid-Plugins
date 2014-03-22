document = {
    elements: {},

    getElementById: function (id) {
        var element = this.elements[id];
        if (element === undefined) {
            element = {};
            this.elements[id] = element;
        }
        return element;
    }
};