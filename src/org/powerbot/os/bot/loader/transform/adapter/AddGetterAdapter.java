package org.powerbot.os.bot.loader.transform.adapter;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class AddGetterAdapter extends ClassVisitor implements Opcodes {
	public static class Field {
		public int getter_access;
		public String getter_name;
		public String getter_desc;
		public String owner;
		public String name;
		public String desc;
		public int overflow;
		public long overflow_val;
	}

	private final boolean virtual;
	private final Field[] fields;
	private String owner;

	public AddGetterAdapter(final ClassVisitor delegate, final boolean virtual, final Field[] fields) {
		super(Opcodes.ASM4, delegate);
		this.virtual = virtual;
		this.fields = fields;
	}

	@Override
	public void visit(
			final int version,
			final int access,
			final String name,
			final String signature,
			final String superName,
			final String[] interfaces) {
		owner = name;
		super.visit(version, access, name, signature, superName, interfaces);
	}

	@Override
	public void visitEnd() {
		for (final Field f : fields) {
			visitGetter(f.getter_access, f.getter_name, f.getter_desc, virtual ? null : f.owner, f.name, f.desc, f.overflow, f.overflow_val);
		}
		super.visitEnd();
	}

	private void visitGetter(
			final int getter_access,
			final String getter_name,
			final String getter_desc,
			final String owner,
			final String name,
			final String desc,
			final int overflow, final long overflow_val) {
		final MethodVisitor mv = super.visitMethod(getter_access, getter_name, getter_desc, null, null);
		mv.visitCode();
		if (owner == null) {
			mv.visitVarInsn(ALOAD, 0);
			mv.visitFieldInsn(GETFIELD, this.owner, name, desc);
		} else {
			mv.visitFieldInsn(GETSTATIC, owner, name, desc);
		}
		switch (overflow) {
		case 1:
			if (overflow_val >= -1 && overflow_val <= 5) {
				mv.visitInsn(Opcodes.ICONST_0 + (int) overflow_val);
			} else if (overflow_val >= Byte.MIN_VALUE && overflow_val <= Byte.MAX_VALUE) {
				mv.visitIntInsn(Opcodes.BIPUSH, (int) overflow_val);
			} else if (overflow_val >= Short.MIN_VALUE && overflow_val <= Short.MAX_VALUE) {
				mv.visitIntInsn(Opcodes.SIPUSH, (int) overflow_val);
			} else {
				mv.visitLdcInsn(new Integer((int) overflow_val));
			}
			mv.visitInsn(Opcodes.IMUL);
			break;
		case 2:
			mv.visitLdcInsn(new Long(overflow_val));
			mv.visitInsn(Opcodes.LMUL);
			break;
		default:
			break;
		}
		final int op = getReturnOpcode(desc);
		mv.visitInsn(op);
		mv.visitMaxs(op == LRETURN || op == DRETURN ?
				overflow == 2 ? 4 : overflow == 1 ? 3 : 2 :
				overflow == 2 ? 3 : overflow == 1 ? 2 : 1,
				(getter_access & ACC_STATIC) == 0 ? 1 : 0);
		mv.visitEnd();
	}

	private int getReturnOpcode(String desc) {
		desc = desc.substring(desc.indexOf(")") + 1);
		if (desc.length() > 1) {
			return ARETURN;
		}
		final char c = desc.charAt(0);
		switch (c) {
		case 'I':
		case 'Z':
		case 'B':
		case 'S':
		case 'C':
			return IRETURN;
		case 'J':
			return LRETURN;
		case 'F':
			return FRETURN;
		case 'D':
			return DRETURN;
		}
		throw new IllegalArgumentException("bad_return");
	}
}
