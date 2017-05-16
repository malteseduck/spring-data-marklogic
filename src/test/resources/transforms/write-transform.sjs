function updateName(context, params, content) {
    var obj = content.toObject();
    obj.name = "Override Master Write";
    return obj;
}

exports.transform = updateName;