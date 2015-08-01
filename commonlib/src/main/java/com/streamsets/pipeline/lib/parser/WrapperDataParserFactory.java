/**
 * (c) 2014 StreamSets, Inc. All rights reserved. May not
 * be copied, modified, or distributed in whole or part without
 * written consent of StreamSets, Inc.
 */
package com.streamsets.pipeline.lib.parser;

import com.streamsets.pipeline.api.Record;
import com.streamsets.pipeline.api.StageException;
import com.streamsets.pipeline.lib.util.ExceptionUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

public class WrapperDataParserFactory extends DataParserFactory {
  private final DataParserFactory factory;

  public WrapperDataParserFactory(DataParserFactory factory) {
    super(factory.getSettings());
    this.factory = factory;
  }

  @Override
  public DataParser getParser(String id, byte[] data, int offset, int len) throws DataParserException {
    return new WrapperDataParser(factory.getParser(id, data, offset, len));
  }

  @Override
  public DataParser getParser(String id, byte[] data) throws DataParserException {
    return new WrapperDataParser(factory.getParser(id, data));
  }

  @Override
  public DataParser getParser(String id, String data) throws DataParserException {
    return new WrapperDataParser(factory.getParser(id, data));
  }

  @Override
  public DataParser getParser(String id, Reader reader) throws DataParserException {
    return new WrapperDataParser(factory.getParser(id, reader));
  }

  @Override
  public DataParser getParser(File file, String fileOffset) throws DataParserException {
    return new WrapperDataParser(factory.getParser(file, fileOffset));
  }

  @Override
  public DataParser getParser(String id, InputStream is, long offset) throws DataParserException {
    return new WrapperDataParser(factory.getParser(id, is, offset));
  }

  @Override
  public DataParser getParser(String id, Reader reader, long offset) throws DataParserException {
    return new WrapperDataParser(factory.getParser(id, reader, offset));
  }


  private static class WrapperDataParser implements DataParser {
    private final DataParser dataParser;


    public WrapperDataParser(DataParser dataParser) {
      this.dataParser = dataParser;
    }

    @Override
    public Record parse() throws IOException, DataParserException {
      try {
        return dataParser.parse();
      } catch (Throwable ex) {
        ExceptionUtils.throwUndeclared(normalizeException(ex));
      }
      return null; //unreacheable
    }

    @Override
    public String getOffset() throws DataParserException, IOException {
      try {
        return dataParser.getOffset();
      } catch (Throwable ex) {
        ExceptionUtils.throwUndeclared(normalizeException(ex));
      }
      return null; //unreacheable
    }

    @Override
    public void close() throws IOException {
      try {
        dataParser.close();
      } catch (Throwable ex) {
        ExceptionUtils.throwUndeclared(normalizeException(ex));
      }
    }

    Throwable normalizeException(Throwable ex) {
      if (!(ex instanceof IOException) && !(ex instanceof DataParserException)) {
        if (ex.getCause() != null) {
          ex = ex.getCause();
          if (!(ex instanceof IOException) && !(ex instanceof DataParserException)) {
            if (ex instanceof StageException) {
              StageException seCause = (StageException) ex;
              ex = new DataParserException(seCause.getErrorCode(), seCause.getParams());
            }
          }
        }
        ex = new DataParserException(Errors.DATA_PARSER_02, ex.toString(), ex);
      }
      return ex;
    }

  }

}