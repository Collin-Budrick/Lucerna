package net.lucerna.render.resources;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

public final class ShaderLayoutValidator {
    private static final Pattern ATTACHMENT_NAME_FORMAT =
            Pattern.compile("lucerna\\.[a-z][a-z0-9_]*\\.[A-Za-z][A-Za-z0-9_]*");

    private ShaderLayoutValidator() {
    }

    public static ShaderLayoutValidationReport validate(ShaderLayout layout) {
        Objects.requireNonNull(layout, "layout");
        List<ShaderLayoutValidationFinding> findings = new ArrayList<>();

        validateRoot(layout, findings);

        Map<String, ShaderPassDescriptor> passesById = validatePasses(layout.passes(), findings);
        Map<String, DescriptorBindingSlot> bindingsByName = validateDescriptorSets(layout.descriptorSets(), findings);
        Map<String, ShaderAttachment> attachmentsByName = validateAttachments(layout.attachments(), passesById, findings);

        Set<String> knownResources = new LinkedHashSet<>(bindingsByName.keySet());
        knownResources.addAll(attachmentsByName.keySet());
        Set<String> descriptorSetNames = descriptorSetNames(layout.descriptorSets());

        validatePassReferences(layout.passes(), descriptorSetNames, knownResources, bindingsByName, attachmentsByName, findings);

        return new ShaderLayoutValidationReport(findings);
    }

    private static void validateRoot(ShaderLayout layout, List<ShaderLayoutValidationFinding> findings) {
        if (layout.schemaVersion() != ShaderLayout.SUPPORTED_SCHEMA_VERSION) {
            findings.add(ShaderLayoutValidationFinding.error(
                    "layout.schema.unsupported",
                    "$.schemaVersion",
                    "Unsupported shader layout schema version " + layout.schemaVersion()
            ));
        }
        if (!ShaderLayout.LUCERNA_NAMESPACE.equals(layout.namespace())) {
            findings.add(ShaderLayoutValidationFinding.error(
                    "layout.namespace.mismatch",
                    "$.namespace",
                    "Expected namespace '" + ShaderLayout.LUCERNA_NAMESPACE + "'"
            ));
        }
    }

    private static Map<String, ShaderPassDescriptor> validatePasses(
            List<ShaderPassDescriptor> passes,
            List<ShaderLayoutValidationFinding> findings
    ) {
        Map<String, ShaderPassDescriptor> passesById = new LinkedHashMap<>();
        Set<Integer> numericIds = new LinkedHashSet<>();
        Set<Integer> executionOrders = new LinkedHashSet<>();

        for (int i = 0; i < passes.size(); i++) {
            ShaderPassDescriptor pass = passes.get(i);
            String path = "$.passes[" + i + "]";
            String passId = pass.id().value();

            if (passesById.putIfAbsent(passId, pass) != null) {
                findings.add(ShaderLayoutValidationFinding.error(
                        "pass.id.duplicate",
                        path + ".id",
                        "Duplicate pass id '" + passId + "'"
                ));
            }
            if (!numericIds.add(pass.numericId())) {
                findings.add(ShaderLayoutValidationFinding.error(
                        "pass.numeric_id.duplicate",
                        path + ".numericId",
                        "Duplicate numeric pass id " + pass.numericId()
                ));
            }
            if (!executionOrders.add(pass.executionOrder())) {
                findings.add(ShaderLayoutValidationFinding.warning(
                        "pass.execution_order.duplicate",
                        path + ".executionOrder",
                        "Multiple passes use execution order " + pass.executionOrder()
                ));
            }
            if (!pass.id().matchesLayoutFormat()) {
                findings.add(ShaderLayoutValidationFinding.error(
                        "pass.id.format",
                        path + ".id",
                        "Pass ids must use lucerna.<stage>.<name>"
                ));
            }
            if (!pass.id().matchesStage(pass.stage())) {
                findings.add(ShaderLayoutValidationFinding.error(
                        "pass.stage.mismatch",
                        path + ".stage",
                        "Pass stage must match the stage segment in '" + passId + "'"
                ));
            }
            if (!pass.stage().equals(pass.directory())) {
                findings.add(ShaderLayoutValidationFinding.warning(
                        "pass.directory.mismatch",
                        path + ".directory",
                        "Pass directory usually matches the pass stage for shader asset ownership"
                ));
            }
            if (pass.type() == ShaderPassType.UNKNOWN) {
                findings.add(ShaderLayoutValidationFinding.error(
                        "pass.type.unknown",
                        path + ".type",
                        "Pass type must be graphics or compute"
                ));
            }
            if (!pass.placeholderShader().startsWith(pass.directory() + "/")) {
                findings.add(ShaderLayoutValidationFinding.error(
                        "pass.placeholder.directory",
                        path + ".placeholderShader",
                        "Placeholder shader must stay under the pass directory"
                ));
            }
            if (!pass.sideEffectFreePlaceholder()) {
                findings.add(ShaderLayoutValidationFinding.error(
                        "pass.placeholder.side_effect_free",
                        path + ".sideEffectFreePlaceholder",
                        "Placeholder shaders are required to remain side-effect free"
                ));
            }
        }

        return passesById;
    }

    private static Map<String, DescriptorBindingSlot> validateDescriptorSets(
            List<ShaderDescriptorSet> descriptorSets,
            List<ShaderLayoutValidationFinding> findings
    ) {
        Map<String, DescriptorBindingSlot> bindingsByName = new LinkedHashMap<>();
        Set<Integer> setNumbers = new LinkedHashSet<>();
        Set<String> setNames = new LinkedHashSet<>();

        for (int i = 0; i < descriptorSets.size(); i++) {
            ShaderDescriptorSet descriptorSet = descriptorSets.get(i);
            String path = "$.descriptorSets[" + i + "]";
            if (!setNumbers.add(descriptorSet.set())) {
                findings.add(ShaderLayoutValidationFinding.error(
                        "descriptor_set.number.duplicate",
                        path + ".set",
                        "Duplicate descriptor set number " + descriptorSet.set()
                ));
            }
            if (!setNames.add(descriptorSet.name())) {
                findings.add(ShaderLayoutValidationFinding.error(
                        "descriptor_set.name.duplicate",
                        path + ".name",
                        "Duplicate descriptor set name '" + descriptorSet.name() + "'"
                ));
            }

            Set<Integer> bindingNumbers = new LinkedHashSet<>();
            for (int bindingIndex = 0; bindingIndex < descriptorSet.bindings().size(); bindingIndex++) {
                ShaderDescriptorBinding binding = descriptorSet.bindings().get(bindingIndex);
                String bindingPath = path + ".bindings[" + bindingIndex + "]";
                if (!bindingNumbers.add(binding.binding())) {
                    findings.add(ShaderLayoutValidationFinding.error(
                            "descriptor_binding.number.duplicate",
                            bindingPath + ".binding",
                            "Duplicate binding " + binding.binding() + " in descriptor set '" + descriptorSet.name() + "'"
                    ));
                }
                if (bindingsByName.putIfAbsent(binding.name(), new DescriptorBindingSlot(descriptorSet.name(), binding)) != null) {
                    findings.add(ShaderLayoutValidationFinding.error(
                            "descriptor_binding.name.duplicate",
                            bindingPath + ".name",
                            "Duplicate descriptor binding resource name '" + binding.name() + "'"
                    ));
                }
            }
        }

        return bindingsByName;
    }

    private static Map<String, ShaderAttachment> validateAttachments(
            List<ShaderAttachment> attachments,
            Map<String, ShaderPassDescriptor> passesById,
            List<ShaderLayoutValidationFinding> findings
    ) {
        Map<String, ShaderAttachment> attachmentsByName = new LinkedHashMap<>();
        for (int i = 0; i < attachments.size(); i++) {
            ShaderAttachment attachment = attachments.get(i);
            String path = "$.attachments[" + i + "]";
            if (attachmentsByName.putIfAbsent(attachment.name(), attachment) != null) {
                findings.add(ShaderLayoutValidationFinding.error(
                        "attachment.name.duplicate",
                        path + ".name",
                        "Duplicate attachment name '" + attachment.name() + "'"
                ));
            }
            if (!ATTACHMENT_NAME_FORMAT.matcher(attachment.name()).matches()) {
                findings.add(ShaderLayoutValidationFinding.warning(
                        "attachment.name.format",
                        path + ".name",
                        "Attachment names should use lucerna.<stage>.<name>"
                ));
            }
            if (!passesById.containsKey(attachment.ownerPass().value())) {
                findings.add(ShaderLayoutValidationFinding.error(
                        "attachment.owner.missing",
                        path + ".ownerPass",
                        "Attachment owner pass '" + attachment.ownerPass().value() + "' is not declared"
                ));
            }
            for (int consumerIndex = 0; consumerIndex < attachment.consumers().size(); consumerIndex++) {
                String consumer = attachment.consumers().get(consumerIndex);
                if (isLucernaReference(consumer) && !passesById.containsKey(consumer)) {
                    findings.add(ShaderLayoutValidationFinding.error(
                            "attachment.consumer.missing",
                            path + ".consumers[" + consumerIndex + "]",
                            "Attachment consumer pass '" + consumer + "' is not declared"
                    ));
                }
            }
        }
        return attachmentsByName;
    }

    private static void validatePassReferences(
            List<ShaderPassDescriptor> passes,
            Set<String> descriptorSetNames,
            Set<String> knownResources,
            Map<String, DescriptorBindingSlot> bindingsByName,
            Map<String, ShaderAttachment> attachmentsByName,
            List<ShaderLayoutValidationFinding> findings
    ) {
        for (int i = 0; i < passes.size(); i++) {
            ShaderPassDescriptor pass = passes.get(i);
            String path = "$.passes[" + i + "]";

            for (int setIndex = 0; setIndex < pass.descriptorSets().size(); setIndex++) {
                String descriptorSet = pass.descriptorSets().get(setIndex);
                if (!descriptorSetNames.contains(descriptorSet)) {
                    findings.add(ShaderLayoutValidationFinding.error(
                            "pass.descriptor_set.missing",
                            path + ".descriptorSets[" + setIndex + "]",
                            "Pass references undeclared descriptor set '" + descriptorSet + "'"
                    ));
                }
            }

            validateResourceReferences(path, "reads", pass.reads(), knownResources, bindingsByName, attachmentsByName, pass, findings);
            validateResourceReferences(path, "writes", pass.writes(), knownResources, bindingsByName, attachmentsByName, pass, findings);
        }
    }

    private static void validateResourceReferences(
            String passPath,
            String field,
            List<String> references,
            Set<String> knownResources,
            Map<String, DescriptorBindingSlot> bindingsByName,
            Map<String, ShaderAttachment> attachmentsByName,
            ShaderPassDescriptor pass,
            List<ShaderLayoutValidationFinding> findings
    ) {
        boolean writes = "writes".equals(field);
        for (int i = 0; i < references.size(); i++) {
            String reference = references.get(i);
            String path = passPath + "." + field + "[" + i + "]";
            if (!knownResources.contains(reference)) {
                findings.add(ShaderLayoutValidationFinding.error(
                        "pass.resource.missing",
                        path,
                        "Pass references undeclared resource '" + reference + "'"
                ));
                continue;
            }

            DescriptorBindingSlot bindingSlot = bindingsByName.get(reference);
            if (bindingSlot != null) {
                ShaderDescriptorBinding binding = bindingSlot.binding();
                if (!pass.usesDescriptorSet(bindingSlot.descriptorSetName())) {
                    findings.add(ShaderLayoutValidationFinding.error(
                            "pass.resource.descriptor_set_not_bound",
                            path,
                            "Pass references descriptor resource '" + reference
                                    + "' without declaring descriptor set '" + bindingSlot.descriptorSetName() + "'"
                    ));
                }
                if (writes && !binding.writable()) {
                    findings.add(ShaderLayoutValidationFinding.error(
                            "pass.resource.write_not_writable",
                            path,
                            "Descriptor binding '" + reference + "' is not writable"
                    ));
                } else if (!writes && !binding.readable()) {
                    findings.add(ShaderLayoutValidationFinding.error(
                            "pass.resource.read_not_readable",
                            path,
                            "Descriptor binding '" + reference + "' is not readable"
                    ));
                }
            }

            ShaderAttachment attachment = attachmentsByName.get(reference);
            if (writes && attachment != null && !attachment.isOwnedBy(pass.id())) {
                findings.add(ShaderLayoutValidationFinding.error(
                        "pass.attachment.owner_mismatch",
                        path,
                        "Pass writes attachment '" + reference + "' owned by '" + attachment.ownerPass().value() + "'"
                ));
            }
        }
    }

    private static Set<String> descriptorSetNames(List<ShaderDescriptorSet> descriptorSets) {
        Set<String> names = new LinkedHashSet<>();
        for (ShaderDescriptorSet descriptorSet : descriptorSets) {
            names.add(descriptorSet.name());
        }
        return names;
    }

    private static boolean isLucernaReference(String value) {
        return value.startsWith("lucerna.");
    }

    private record DescriptorBindingSlot(String descriptorSetName, ShaderDescriptorBinding binding) {
    }
}
